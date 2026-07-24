package com.deeprobotics.station.openapi.sdk.transport.okhttp;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException;
import com.deeprobotics.station.openapi.sdk.http.HttpConnectionException;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTimeoutException;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;

/**
 * 基于 OkHttp 的 SDK 默认 HTTP 传输实现。
 *
 * <p>这个类位于独立 transport 模块，是 {@code sdk-core} 与 OkHttp 之间的边界适配层：
 * core 只认识 {@link HttpTransport}、{@link HttpRequest} 和 {@link HttpResponse}，
 * 不直接依赖 OkHttp 类型；本类负责把 core 已经准备好的 URL、header、JSON body 和超时配置翻译成
 * OkHttp 请求。</p>
 *
 * <p>实例必须通过 {@link OkHttpTransportFactory} 创建。Factory 统一应用安全配置并明确底层 client
 * 的资源所有权，避免客户绕过禁止透明重试、禁止重定向或关闭责任规则。</p>
 *
 * <p>鉴权、签名、请求重试、{@code R<T>} 解包、业务异常转换由 core 的 executor 统一处理。
 * transport 层只做底层 I/O：发送请求、完整保留 2xx JSON、有界保留错误体、流式写下载内容，
 * 并把网络层异常归类成 core 可识别的 transport 异常。</p>
 */
public final class OkHttpTransport implements HttpTransport {

    /** 所有 OpenAPI HTTP 调用都以 JSON POST 发送；下载接口的请求体同样是 JSON，响应体才是二进制流。 */
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    /** 普通 JSON 接口的默认读超时，和 SDK builder 默认值保持一致。 */
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);
    /**
     * transport 直连且 {@link HttpRequest#readTimeout()} 为 null 时的下载读超时兜底值。
     *
     * <p>公共客户端执行器始终传入全局或请求级超时，因此正常 SDK 下载默认仍是全局 10 秒，
     * 不会自动使用这里的 60 秒；长下载需要客户通过 RequestOptions 显式放大 read/call timeout。</p>
     */
    private static final Duration DEFAULT_DOWNLOAD_READ_TIMEOUT = Duration.ofSeconds(60);
    /** call timeout 是单次 OkHttp call 的总耗时上限，避免 DNS、连接、读写阶段无限等待。 */
    private static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(15);
    /** 8 KiB 是流式拷贝常见缓冲大小，避免一次性把下载文件读入内存。 */
    private static final int BUFFER_SIZE = 8192;
    /** 失败或协议异常响应最多保留 64 KiB，足够诊断且避免网关大错误页造成无界内存占用。 */
    private static final int ERROR_BODY_LIMIT = 64 * 1024;

    private final OkHttpClient client;
    private final boolean clientOwnedBySdk;

    OkHttpTransport(OkHttpClient client, boolean clientOwnedBySdk) {
        this.client = Objects.requireNonNull(client, "client is required");
        this.clientOwnedBySdk = clientOwnedBySdk;
    }

    @Override
    public HttpResponse postJson(HttpRequest request) {
        HttpRequest requiredRequest = Objects.requireNonNull(request, "request is required");
        Request okRequest = buildPostRequest(requiredRequest);
        try (Response response = clientFor(requiredRequest, false).newCall(okRequest).execute()) {
            ResponseBody body = response.body();
            byte[] responseBody;
            if (body == null) {
                responseBody = new byte[0];
            } else if (response.isSuccessful()) {
                // 2xx R<T> 必须完整交给 core 解析，不能把成功业务数据截断成伪造 JSON 错误。
                responseBody = body.bytes();
            } else {
                responseBody = readLimited(body.byteStream(), ERROR_BODY_LIMIT);
            }
            return new HttpResponse(response.code(), headers(response), responseBody);
        } catch (InterruptedIOException ex) {
            // OkHttp 的超时异常属于 InterruptedIOException 分支，单独映射后 core 才能设置 resultUnknown。
            throw new HttpTimeoutException("OpenAPI HTTP request timed out for " + path(requiredRequest), ex);
        } catch (IOException ex) {
            // DNS、连接失败、TLS 握手失败、连接中断等统一交给 core 按操作意图转换为 SDK transport 异常。
            throw new HttpConnectionException("OpenAPI HTTP request failed for " + path(requiredRequest), ex);
        }
    }

    @Override
    public HttpResponse download(HttpRequest request, OutputStream outputStream) {
        HttpRequest requiredRequest = Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(outputStream, "outputStream is required");
        Request okRequest = buildPostRequest(requiredRequest);
        try (Response response = clientFor(requiredRequest, true).newCall(okRequest).execute()) {
            Map<String, List<String>> headers = headers(response);
            ResponseBody body = response.body();
            if (body == null) {
                return new HttpResponse(response.code(), headers, new byte[0]);
            }

            if (!response.isSuccessful() || !isBinaryDownloadResponse(body)) {
                /*
                 * 平台成功文件明确使用 application/octet-stream。JSON、problem+json、HTML、缺失或其他
                 * Content-Type 都作为有限诊断体返回 executor，绝不能先写入临时文件；否则 JSON 错误体
                 * 会在 transport 层被消耗，core 只能看到空 body，甚至可能把网关登录页当成成功文件。
                 */
                byte[] errorBody = readLimited(body.byteStream(), ERROR_BODY_LIMIT);
                return new HttpResponse(response.code(), headers, errorBody);
            }

            // 只有确认是成功的 octet-stream 后，才把响应体流式写到调用方提供的 OutputStream。
            streamToOutput(body.byteStream(), outputStream, requiredRequest);
            return new HttpResponse(response.code(), headers, new byte[0]);
        } catch (StationOpenApiDownloadException ex) {
            throw ex;
        } catch (InterruptedIOException ex) {
            throw new HttpTimeoutException("OpenAPI download request timed out for " + path(requiredRequest), ex);
        } catch (IOException ex) {
            throw new HttpConnectionException("OpenAPI download request failed for " + path(requiredRequest), ex);
        }
    }

    @Override
    public void close() {
        if (!clientOwnedBySdk) {
            return;
        }
        // SDK 自建 client 才释放底层线程池和连接池；客户注入 client 时由客户生命周期管理。
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        Cache cache = client.cache();
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException ex) {
                // cache 清理采用尽力而为策略，close 不应因缓存关闭失败覆盖客户此前的主要调用结果。
            }
        }
    }

    /**
     * 为单次请求派生带本次超时设置的 OkHttpClient。
     *
     * <p>OkHttpClient 本身是不可变对象，{@code newBuilder()} 派生的新实例仍复用原连接池和 dispatcher。
     * 这样既能支持 RequestOptions 的单次超时覆盖，也不会为每次调用创建新的连接池。</p>
     */
    private OkHttpClient clientFor(HttpRequest request, boolean download) {
        // executor 会传入全局或 RequestOptions 覆盖值；只有 transport 直连并传 null 时才使用本类兜底值。
        // OkHttpClient 是不可变配置对象，newBuilder() 会复用连接池和 dispatcher，不会新建网络资源池。
        Duration readTimeout = request.readTimeout() == null
                ? (download ? DEFAULT_DOWNLOAD_READ_TIMEOUT : DEFAULT_READ_TIMEOUT)
                : request.readTimeout();
        Duration callTimeout = request.callTimeout() == null ? DEFAULT_CALL_TIMEOUT : request.callTimeout();

        return client.newBuilder()
                .readTimeout(OkHttpTimeouts.toMillis(readTimeout, "readTimeout"), TimeUnit.MILLISECONDS)
                .callTimeout(OkHttpTimeouts.toMillis(callTimeout, "callTimeout"), TimeUnit.MILLISECONDS)
                .addInterceptor(new SingleProceedInterceptor())
                .build();
    }

    /**
     * 将 core 已经准备好的请求快照转换为 OkHttp POST 请求。
     *
     * <p>这里不做鉴权、不追加公共字段、不重新序列化 JSON。任何在 transport 层改写 body 的行为，
     * 都可能让签名源和实际发送内容不一致。</p>
     */
    private static Request buildPostRequest(HttpRequest request) {
        // 到达 transport 前，core 已经完成请求模型映射、鉴权 header/body 注入、签名和 JSON 序列化。
        // 这里不再改写 body，避免签名源和实际发送内容不一致。
        Request.Builder builder = new Request.Builder()
                .url(request.url().toString())
                .post(new SingleExchangeRequestBody(request.body()));
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            // 使用 header() 覆盖同名 header，确保 X-Request-Id、Content-Type 等基础值最终唯一。
            builder.header(header.getKey(), header.getValue());
        }
        return builder.build();
    }

    /**
     * 将 OkHttp 响应 header 转成 core 的多值 header 结构。
     */
    private static Map<String, List<String>> headers(Response response) {
        return response.headers().toMultimap();
    }

    /**
     * 判断下载响应是否符合平台明确的二进制成功契约。
     *
     * <p>只有 {@code application/octet-stream} 可以流式写出；JSON、HTML、缺失或其他类型都保留为
     * 有界诊断 body，让 executor 决定抛业务、序列化或下载协议异常。</p>
     */
    private static boolean isBinaryDownloadResponse(ResponseBody body) {
        MediaType contentType = body.contentType();
        return contentType != null
                && "application".equals(contentType.type())
                && "octet-stream".equals(contentType.subtype());
    }

    /**
     * 读取最多 {@code maxBytes} 字节错误体。
     *
     * <p>该方法只用于下载失败或 JSON 错误响应，不用于正常文件下载。正常下载必须流式写出，
     * 不能把整个文件读入内存。</p>
     */
    private static byte[] readLimited(InputStream inputStream, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, BUFFER_SIZE));
        byte[] buffer = new byte[BUFFER_SIZE];
        int remaining = maxBytes;
        while (remaining > 0) {
            int read = inputStream.read(buffer, 0, Math.min(buffer.length, remaining));
            if (read == -1) {
                break;
            }
            output.write(buffer, 0, read);
            remaining -= read;
        }
        return output.toByteArray();
    }

    /**
     * 将成功的二进制下载响应流式写入调用方输出流。
     *
     * <p>写调用方输出流失败时，SDK 无法判断目标文件是否已经写入部分字节，因此必须抛出
     * {@code resultUnknown=true} 的下载异常，让客户侧自行清理或复核目标文件。</p>
     */
    private static void streamToOutput(InputStream inputStream, OutputStream outputStream, HttpRequest request)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            try {
                outputStream.write(buffer, 0, read);
            } catch (IOException | RuntimeException ex) {
                /*
                 * 客户 OutputStream 既可能抛 IOException，也可能通过第三方适配器抛 unchecked 异常。
                 * 两者都可能发生在已写出部分字节之后，必须统一包装、脱敏并标记 resultUnknown=true。
                 */
                throw new StationOpenApiDownloadException("Failed to write OpenAPI download response stream",
                        path(request), request.headers().get("X-Request-Id"), null, true, ex);
            }
        }
    }

    /**
     * 提取用于异常上下文的 path。
     *
     * <p>异常里只放 path，不放完整 URL，避免把客户 endpoint、网关路径或未来 query 信息带进日志。</p>
     */
    private static String path(HttpRequest request) {
        String rawPath = request.url().getRawPath();
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }
        return rawPath;
    }

    /**
     * 把请求体声明为不可重放，阻止 OkHttp 的 408/421/503、认证和重定向 follow-up 再次发送 POST。
     *
     * <p>底层字节其实可以再次读取，但业务语义不允许 transport 自行决定重发；需要重试时只能回到
     * core executor，按 OperationIntent 判断并重新生成 timestamp/nonce/sign。</p>
     */
    private static final class SingleExchangeRequestBody extends RequestBody {
        private final byte[] body;

        private SingleExchangeRequestBody(byte[] body) {
            this.body = body;
        }

        @Override
        public MediaType contentType() {
            return JSON_MEDIA_TYPE;
        }

        @Override
        public long contentLength() {
            return body.length;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            sink.write(body);
        }

        @Override
        public boolean isOneShot() {
            return true;
        }
    }

    /**
     * 防止客户 application interceptor 在一次 SDK transport 调用中自行多次调用 chain.proceed()。
     *
     * <p>该拦截器按请求创建并排在客户 interceptor 之后：客户第一次 proceed 可以正常进入网络链路；
     * 同一个客户 interceptor 若再次 proceed，会在产生第二个网络请求前失败。OkHttp 内建 follow-up
     * 不会重新经过 application interceptor，因此由 {@link SingleExchangeRequestBody#isOneShot()} 补齐。</p>
     */
    private static final class SingleProceedInterceptor implements Interceptor {
        private final AtomicBoolean proceeded = new AtomicBoolean();

        @Override
        public Response intercept(Chain chain) throws IOException {
            if (!proceeded.compareAndSet(false, true)) {
                throw new IOException("Station OpenAPI transport forbids multiple proceeds per SDK attempt");
            }
            return chain.proceed(chain.request());
        }
    }

}
