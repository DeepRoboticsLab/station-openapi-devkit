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
import org.jetbrains.annotations.NotNull;

/**
 * 基于 OkHttp 的 SDK 默认 HTTP 传输实现。
 *
 * <p>将 core 的请求快照转换为 OkHttp 请求，使 core 无需依赖 OkHttp 类型。</p>
 *
 * <p>通过 {@link OkHttpTransportFactory} 创建，统一禁用透明重试和重定向，并确定客户端资源所有权。</p>
 *
 * <p>本类负责单次 I/O、流式下载和网络异常分类；鉴权、重试及业务响应处理由 core 执行器负责。</p>
 */
public final class OkHttpTransport implements HttpTransport {

    /** 下载接口同样发送 JSON 请求体。 */
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    /** 单次阻塞读取的默认等待时限，普通请求和下载均为 10 秒。 */
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);
    /** 普通 JSON 调用总超时，包含 DNS、连接和读写阶段；下载默认不设总时限。 */
    private static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(15);
    private static final int BUFFER_SIZE = 8192;
    /** 限制错误响应大小，避免网关错误页造成无界内存占用。 */
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
        try (Response response = clientFor(requiredRequest, DEFAULT_CALL_TIMEOUT).newCall(okRequest).execute()) {
            ResponseBody body = response.body();
            byte[] responseBody;
            if (body == null) {
                responseBody = new byte[0];
            } else if (response.isSuccessful()) {
                // 成功 JSON 必须完整解析，不适用错误体的截断上限。
                responseBody = body.bytes();
            } else {
                responseBody = readLimited(body.byteStream(), ERROR_BODY_LIMIT);
            }
            return new HttpResponse(response.code(), headers(response), responseBody);
        } catch (InterruptedIOException ex) {
            throw new HttpTimeoutException("OpenAPI HTTP request timed out for " + path(requiredRequest), ex);
        } catch (IOException ex) {
            throw new HttpConnectionException("OpenAPI HTTP request failed for " + path(requiredRequest), ex);
        }
    }

    @Override
    public HttpResponse download(HttpRequest request, OutputStream outputStream) {
        HttpRequest requiredRequest = Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(outputStream, "outputStream is required");
        Request okRequest = buildPostRequest(requiredRequest);
        try (Response response = clientFor(requiredRequest, Duration.ZERO).newCall(okRequest).execute()) {
            Map<String, List<String>> headers = headers(response);
            ResponseBody body = response.body();
            if (body == null) {
                return new HttpResponse(response.code(), headers, new byte[0]);
            }

            if (!response.isSuccessful() || !isBinaryDownloadResponse(body)) {
                // 非二进制成功响应留给 core 解析，避免把 JSON 错误体或网关页面写成文件。
                byte[] errorBody = readLimited(body.byteStream(), ERROR_BODY_LIMIT);
                return new HttpResponse(response.code(), headers, errorBody);
            }

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
        // 仅释放 SDK 自建客户端的资源。
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        Cache cache = client.cache();
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException ex) {
                // 缓存清理失败不覆盖调用结果。
            }
        }
    }

    /**
     * 派生本次调用的超时配置，{@code newBuilder()} 复用原连接池和 dispatcher。
     */
    private OkHttpClient clientFor(HttpRequest request, Duration defaultCallTimeout) {
        Duration readTimeout = request.readTimeout() == null
                ? DEFAULT_READ_TIMEOUT : request.readTimeout();
        Duration callTimeout = request.callTimeout() == null ? defaultCallTimeout : request.callTimeout();

        return client.newBuilder()
                .readTimeout(OkHttpTimeouts.toMillis(readTimeout, "readTimeout"), TimeUnit.MILLISECONDS)
                .callTimeout(callTimeout.isZero() ? 0 : OkHttpTimeouts.toMillis(callTimeout, "callTimeout"),
                        TimeUnit.MILLISECONDS)
                .addInterceptor(new SingleProceedInterceptor())
                .build();
    }

    /**
     * 原样发送 core 的请求快照，避免改写请求体导致签名不一致。
     */
    private static Request buildPostRequest(HttpRequest request) {
        Request.Builder builder = new Request.Builder()
                .url(request.url().toString())
                .post(new SingleExchangeRequestBody(request.body()));
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            // 覆盖同名请求头，避免重复值。
            builder.header(header.getKey(), header.getValue());
        }
        return builder.build();
    }

    private static Map<String, List<String>> headers(Response response) {
        return response.headers().toMultimap();
    }

    /**
     * 平台下载成功的内容类型为 {@code application/octet-stream}。
     */
    private static boolean isBinaryDownloadResponse(ResponseBody body) {
        MediaType contentType = body.contentType();
        return contentType != null
                && "application".equals(contentType.type())
                && "octet-stream".equals(contentType.subtype());
    }

    /**
     * 最多保留 {@code maxBytes} 字节错误体；成功文件走流式下载。
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
     * 流式写入二进制响应，不关闭调用方输出流。
     *
     * <p>写入失败时可能已输出部分字节，以 {@code resultUnknown=true} 提醒调用方复核目标。</p>
     */
    private static void streamToOutput(InputStream inputStream, OutputStream outputStream, HttpRequest request)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            try {
                outputStream.write(buffer, 0, read);
            } catch (IOException | RuntimeException ex) {
                // 第三方输出流也可能抛出未检查异常，与 I/O 异常统一处理。
                throw new StationOpenApiDownloadException("Failed to write OpenAPI download response stream",
                        path(request), request.headers().get("X-Request-Id"), null, true, ex);
            }
        }
    }

    /**
     * 异常上下文仅保留路径，避免暴露完整 URL。
     */
    private static String path(HttpRequest request) {
        String rawPath = request.url().getRawPath();
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }
        return rawPath;
    }

    /**
     * 声明请求体不可重放，阻止 OkHttp 因 408/421/503、认证或重定向再次发送 POST。
     *
     * <p>即使字节可重复读取，重试也必须由 core 按操作意图决定，并重新鉴权。</p>
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
     * 阻止客户应用拦截器在一次 SDK 尝试中多次调用 {@code chain.proceed()}。
     *
     * <p>按请求创建并排在客户拦截器之后，在第二次网络请求前拒绝调用。OkHttp 内建后续请求
     * 不经过应用拦截器，另由 {@link SingleExchangeRequestBody#isOneShot()} 阻止。</p>
     */
    private static final class SingleProceedInterceptor implements Interceptor {
        private final AtomicBoolean proceeded = new AtomicBoolean();

        @NotNull
        @Override
        public Response intercept(@NotNull Chain chain) throws IOException {
            if (!proceeded.compareAndSet(false, true)) {
                throw new IOException("Station OpenAPI transport forbids multiple proceeds per SDK attempt");
            }
            return chain.proceed(chain.request());
        }
    }

}
