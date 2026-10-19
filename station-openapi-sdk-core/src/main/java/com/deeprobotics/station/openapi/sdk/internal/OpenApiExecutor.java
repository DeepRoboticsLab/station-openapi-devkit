package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.auth.AuthenticatedRequest;
import com.deeprobotics.station.openapi.sdk.auth.AuthProvider;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiBusinessException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiHttpException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTimeoutException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTransportException;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.EndpointResolver;
import com.deeprobotics.station.openapi.sdk.http.HttpConnectionException;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTimeoutException;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
import com.deeprobotics.station.openapi.sdk.retry.RetryPolicy;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * OpenAPI 请求执行器。
 *
 * <p>业务 API 方法最终都应收敛到这个类：请求模型 {@code toMap()}、鉴权、JSON 序列化、
 * HTTP 调用、{@code R<T>} 解包、异常转换、重试和下载处理都在这里统一完成。
 * 这样可以避免每个业务分组各自实现一套签名、错误处理或重试逻辑。</p>
 *
 * <p>该类属于 SDK internal 包，不作为客户公共 API 暴露。默认 HTTP transport 只需要满足
 * {@link HttpTransport} 契约，仍由本类决定“什么时候重试、如何抛 SDK 异常”。</p>
 */
public final class OpenApiExecutor {

    /**
     * SDK 所有普通请求和下载请求使用的 JSON Content-Type。
     */
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    /**
     * 默认期望平台返回 JSON；文件下载成功时平台会返回二进制内容，但错误仍可能是 JSON。
     */
    private static final String ACCEPT_JSON = "application/json";
    /**
     * 已归一化的 OpenAPI endpoint。真正请求 URI 由 {@link EndpointResolver} 与接口 path 组合。
     */
    private final String endpoint;
    /**
     * 当前客户端选定的鉴权 provider。每次请求和每次重试都会通过它重新生成鉴权材料。
     */
    private final AuthProvider authProvider;
    /**
     * HTTP 抽象实现。executor 只依赖接口，不感知具体客户端库。
     */
    private final HttpTransport transport;
    /**
     * JSON 编解码器，负责统一时间格式、未知字段兼容和 UTF-8 序列化。
     */
    private final JsonCodec jsonCodec;
    /**
     * 全局默认读取超时，请求级选项未覆盖时使用。
     */
    private final Duration defaultReadTimeout;
    /**
     * 全局默认调用总超时，请求级选项未覆盖时使用。
     */
    private final Duration defaultCallTimeout;
    /**
     * 下载临时文件存储策略。生产使用系统临时目录，测试可注入失败场景。
     */
    private final DownloadTempFileStore downloadTempFileStore;
    /**
     * SDK User-Agent，帮助平台日志区分调用方自研 HTTP 请求和 SDK 请求。
     */
    private final String userAgent;

    /**
     * 使用默认 Jackson JSON 编解码器创建 executor。
     *
     * @param config 已校验的客户端配置
     * @param authProvider 客户端选定的鉴权实现
     * @param transport 单次 HTTP I/O 实现
     */
    public OpenApiExecutor(StationOpenApiConfig config, AuthProvider authProvider, HttpTransport transport) {
        this(config, authProvider, transport, JsonCodec.defaultCodec());
    }

    /**
     * 允许测试或高级装配注入 JSON codec，生产默认应使用 {@link JsonCodec#defaultCodec()}。
     *
     * @param config 已校验的客户端配置
     * @param authProvider 客户端选定的鉴权实现
     * @param transport 单次 HTTP I/O 实现
     * @param jsonCodec 请求和响应共用的 JSON 编解码器
     */
    public OpenApiExecutor(StationOpenApiConfig config, AuthProvider authProvider, HttpTransport transport,
            JsonCodec jsonCodec) {
        this(config, authProvider, transport, jsonCodec, new DefaultDownloadTempFileStore(null));
    }

    OpenApiExecutor(StationOpenApiConfig config, AuthProvider authProvider, HttpTransport transport,
            JsonCodec jsonCodec, Path downloadTempDirectory) {
        this(config, authProvider, transport, jsonCodec, new DefaultDownloadTempFileStore(downloadTempDirectory));
    }

    OpenApiExecutor(StationOpenApiConfig config, AuthProvider authProvider, HttpTransport transport,
            JsonCodec jsonCodec, DownloadTempFileStore downloadTempFileStore) {
        StationOpenApiConfig requiredConfig = Objects.requireNonNull(config, "config is required");
        this.endpoint = requiredConfig.endpoint();
        this.defaultReadTimeout = requiredConfig.readTimeout();
        this.defaultCallTimeout = requiredConfig.callTimeout();
        this.authProvider = Objects.requireNonNull(authProvider, "authProvider is required");
        this.transport = Objects.requireNonNull(transport, "transport is required");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec is required");
        this.downloadTempFileStore = Objects.requireNonNull(downloadTempFileStore,
                "downloadTempFileStore is required");
        this.userAgent = "station-openapi-java/" + sdkVersion();
    }

    /**
     * 执行普通 JSON OpenAPI 请求。
     *
     * <p>每次重试都会重新调用 {@link #buildRequest(EndpointDefinition, OpenApiRequest, RequestOptions, String)}，
     * 因此请求模型会重新转 map，鉴权 provider 会重新生成 timestamp/nonce/sign。</p>
     *
     * @param <T> 成功响应中的业务数据类型
     * @param endpointDefinition 接口路径、操作名和安全意图元数据
     * @param request 业务请求模型；无业务字段接口可传 {@code null}
     * @param responseType 完整的 {@code ApiResponse<T>} 泛型类型
     * @param options 请求级覆盖选项；传 {@code null} 时使用客户端默认值
     * @return 解包后的业务数据
     */
    public <T> T execute(EndpointDefinition endpointDefinition, OpenApiRequest request,
            TypeRef<ApiResponse<T>> responseType, RequestOptions options) {
        EndpointDefinition endpointDef = Objects.requireNonNull(endpointDefinition, "endpointDefinition is required");
        TypeRef<ApiResponse<T>> type = Objects.requireNonNull(responseType, "responseType is required");
        RequestOptions effectiveOptions = prepareOptions(options);
        String requestId = effectiveOptions.requestId();
        RetryPolicy retryPolicy = RetryPolicy.from(effectiveOptions.retryPolicy(), endpointDef.intent());

        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            try {
                // buildRequest 必须在循环内执行，避免重试复用 nonce/sign。
                HttpRequest httpRequest = buildRequest(endpointDef, request, effectiveOptions, requestId);
                HttpResponse response = postJson(httpRequest, endpointDef, requestId);
                return handleJsonResponse(endpointDef, response, type, requestId);
            } catch (HttpTimeoutException ex) {
                if (retryPolicy.shouldRetry(attempt, ex)) {
                    continue;
                }
                throw timeoutException(endpointDef, requestId, ex);
            } catch (HttpConnectionException ex) {
                if (retryPolicy.shouldRetry(attempt, ex)) {
                    continue;
                }
                throw transportException(endpointDef, requestId, ex);
            }
        }
        throw new IllegalStateException("unreachable retry state");
    }

    /**
     * 执行成功响应必须携带业务 data 的接口。
     *
     * <p>该校验不能放进通用 {@link #execute}：任务下发、任务控制和 dogPos 外层字符串都允许成功响应
     * 没有 data。调用方必须依据具体平台契约显式选择本入口，避免把合法的可空响应误判为协议错误。</p>
     */
    <T> T executeRequiredData(EndpointDefinition endpointDefinition, OpenApiRequest request,
            TypeRef<ApiResponse<T>> responseType, RequestOptions options) {
        EndpointDefinition endpointDef = Objects.requireNonNull(
                endpointDefinition, "endpointDefinition is required");
        RequestOptions effectiveOptions = prepareOptions(options);
        T data = execute(endpointDef, request, responseType, effectiveOptions);
        if (data == null) {
            throw new StationOpenApiSerializationException("OpenAPI response data is required",
                    endpointDef.path(), effectiveOptions.requestId(), endpointDef.operation(), null);
        }
        return data;
    }

    /**
     * 执行文件下载请求。
     *
     * <p>下载不能直接把每次尝试写入调用方 outputStream。第一次尝试可能已经写出部分字节后连接中断，
     * 如果第二次重试继续写入同一个 outputStream，客户会得到拼接出来的坏文件。因此每次尝试先写入
     * SDK 临时文件，只有一次尝试完整成功并确认不是 JSON 错误体后，才复制给调用方。</p>
     *
     * @param endpointDefinition 下载接口路径、操作名和安全意图元数据
     * @param request 下载业务请求模型
     * @param outputStream 调用方提供的最终输出流，执行器不负责关闭
     * @param options 请求级覆盖选项；传 {@code null} 时使用客户端默认值
     */
    public void download(EndpointDefinition endpointDefinition, OpenApiRequest request, OutputStream outputStream,
            RequestOptions options) {
        EndpointDefinition endpointDef = Objects.requireNonNull(endpointDefinition, "endpointDefinition is required");
        Objects.requireNonNull(outputStream, "outputStream is required");
        RequestOptions effectiveOptions = prepareOptions(options);
        String requestId = effectiveOptions.requestId();
        RetryPolicy retryPolicy = RetryPolicy.from(effectiveOptions.retryPolicy(), endpointDef.intent());

        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            Path attemptFile = createDownloadTempFile(endpointDef, requestId);
            try {
                HttpResponse response;
                try (OutputStream attemptOutput = Files.newOutputStream(attemptFile)) {
                    // 和普通请求一样，下载重试也必须重新鉴权，不能复用已发送过的 nonce/sign。
                    HttpRequest httpRequest = buildRequest(endpointDef, request, effectiveOptions, requestId);
                    try {
                        response = requireTransportResponse(
                                transport.download(httpRequest, attemptOutput), endpointDef, requestId, "download");
                    } catch (StationOpenApiDownloadException ex) {
                        // transport 此时写入的是 SDK 为“本次尝试”创建的临时文件，还没有触碰客户 outputStream。
                        // 即使底层已经收到部分二进制响应，失败的也只是临时文件落盘；最终返回给客户的目标流
                        // 仍然没有变化，因此 resultUnknown 必须改为 false，方便客户判断是否需要清理本地文件。
                        throw new StationOpenApiDownloadException("Failed to write OpenAPI download response",
                                endpointDef.path(), requestId, endpointDef.operation(), false, ex);
                    } catch (HttpTimeoutException | HttpConnectionException | StationOpenApiException ex) {
                        throw ex;
                    } catch (RuntimeException ex) {
                        throw unexpectedTransportException(endpointDef, requestId, ex);
                    }
                }
                if (handleDownloadResponse(endpointDef, response, requestId)) {
                    copyDownloadToCaller(attemptFile, outputStream, endpointDef, requestId);
                }
                return;
            } catch (HttpTimeoutException ex) {
                if (retryPolicy.shouldRetry(attempt, ex)) {
                    continue;
                }
                throw timeoutException(endpointDef, requestId, ex);
            } catch (HttpConnectionException ex) {
                if (retryPolicy.shouldRetry(attempt, ex)) {
                    continue;
                }
                throw transportException(endpointDef, requestId, ex);
            } catch (IOException ex) {
                // 这里是打开、写入或关闭 SDK 本次尝试临时文件时发生的本地文件系统失败。
                // 对客户提供的 outputStream 而言还没有产生可见副作用，所以结果未知为 false。
                throw new StationOpenApiDownloadException("Failed to write OpenAPI download response",
                        endpointDef.path(), requestId, endpointDef.operation(), false, ex);
            } finally {
                deleteDownloadTempFile(attemptFile, endpointDef, requestId);
            }
        }
        throw new IllegalStateException("unreachable retry state");
    }

    /**
     * 组装最终 HTTP 请求。
     *
     * <p>顺序很重要：先取业务 map，再鉴权，再序列化 JSON。签名 provider 看到的 body
     * 必须和最终序列化发送的 body 保持一致。</p>
     */
    private HttpRequest buildRequest(EndpointDefinition endpointDefinition, OpenApiRequest request,
            RequestOptions options, String requestId) {
        LinkedHashMap<String, Object> businessBody = request == null ? MapUtils.newBody() : request.toMap();
        AuthenticatedRequest authenticatedRequest = authProvider.authenticate(businessBody);
        LinkedHashMap<String, String> headers = defaultHeaders(requestId);
        headers.putAll(authenticatedRequest.headers());

        byte[] body;
        try {
            body = jsonCodec.writeBytes(authenticatedRequest.body());
        } catch (StationOpenApiSerializationException ex) {
            throw new StationOpenApiSerializationException("Failed to serialize OpenAPI request",
                    endpointDefinition.path(), requestId, endpointDefinition.operation(), ex);
        }

        URI url = EndpointResolver.resolve(endpoint, endpointDefinition.path());
        return new HttpRequest(url, headers, body, effectiveReadTimeout(options), effectiveCallTimeout(options));
    }

    private HttpResponse postJson(HttpRequest request, EndpointDefinition endpointDefinition, String requestId) {
        try {
            HttpResponse response = transport.postJson(request);
            return requireTransportResponse(response, endpointDefinition, requestId, "postJson");
        } catch (HttpTimeoutException | HttpConnectionException | StationOpenApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            /*
             * HttpTransport 是公共扩展点，第三方实现可能抛出未分类的 RuntimeException。此类异常不能
             * 参与自动重试（SDK 无法确认它是否代表可安全重试的连接失败），但仍要进入统一异常体系，
             * 让 message/cause 经过脱敏，并按操作意图保守标记 resultUnknown。
             */
            throw unexpectedTransportException(endpointDefinition, requestId, ex);
        }
    }

    private HttpResponse requireTransportResponse(HttpResponse response,
            EndpointDefinition endpointDefinition, String requestId, String methodName) {
        if (response == null) {
            throw unexpectedTransportException(endpointDefinition, requestId,
                    new IllegalStateException("HttpTransport." + methodName + " returned null"));
        }
        return response;
    }

    /**
     * 处理普通 JSON 响应：先看 HTTP status，再按 {@code R<T>} 解包。
     */
    private <T> T handleJsonResponse(EndpointDefinition endpointDefinition, HttpResponse response,
            TypeRef<ApiResponse<T>> responseType, String requestId) {
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new StationOpenApiHttpException(statusCode, endpointDefinition.path(), requestId,
                    endpointDefinition.operation(), null);
        }

        ApiResponse<T> apiResponse;
        try {
            apiResponse = jsonCodec.read(response.body(), responseType);
        } catch (StationOpenApiSerializationException ex) {
            throw new StationOpenApiSerializationException("Failed to parse OpenAPI response",
                    endpointDefinition.path(), requestId, endpointDefinition.operation(), ex);
        }
        if (apiResponse == null) {
            throw new StationOpenApiSerializationException("OpenAPI response envelope must not be JSON null",
                    endpointDefinition.path(), requestId, endpointDefinition.operation(), null);
        }
        return apiResponse.unwrapOrThrow(endpointDefinition.path(), requestId, endpointDefinition.operation());
    }

    private Path createDownloadTempFile(EndpointDefinition endpointDefinition, String requestId) {
        try {
            return downloadTempFileStore.create();
        } catch (IOException ex) {
            throw new StationOpenApiDownloadException("Failed to create OpenAPI download temp file",
                    endpointDefinition.path(), requestId, endpointDefinition.operation(), false, ex);
        }
    }

    private void deleteDownloadTempFile(Path attemptFile, EndpointDefinition endpointDefinition, String requestId) {
        try {
            downloadTempFileStore.delete(attemptFile);
        } catch (IOException | RuntimeException ex) {
            // 尽力清理每次下载尝试的临时文件，清理失败不能改变原始下载结果或阻断后续重试。
        }
    }

    /**
     * 解释下载响应。
     *
     * <p>正常下载应是二进制流，transport 已经写入临时文件；如果响应是 JSON，
     * 即使 HTTP 2xx，也不能当成文件成功。JSON failure 转业务异常，JSON success envelope
     * 代表平台响应与 transport 契约不匹配，按下载协议异常处理。</p>
     */
    private boolean handleDownloadResponse(EndpointDefinition endpointDefinition, HttpResponse response,
            String requestId) {
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new StationOpenApiHttpException(statusCode, endpointDefinition.path(), requestId,
                    endpointDefinition.operation(), null);
        }
        if (isJsonContentType(response)) {
            ApiResponse<Object> apiResponse;
            try {
                apiResponse = jsonCodec.read(response.body(), new TypeRef<ApiResponse<Object>>() {
                });
            } catch (StationOpenApiSerializationException ex) {
                throw new StationOpenApiSerializationException("Failed to parse OpenAPI download error response",
                        endpointDefinition.path(), requestId, endpointDefinition.operation(), ex);
            }
            if (apiResponse == null) {
                throw new StationOpenApiSerializationException(
                        "OpenAPI download error envelope must not be JSON null",
                        endpointDefinition.path(), requestId, endpointDefinition.operation(), null);
            }
            if (!apiResponse.isSuccessful()) {
                throw new StationOpenApiBusinessException(apiResponse.code(), apiResponse.msg(),
                        endpointDefinition.path(), requestId, endpointDefinition.operation());
            }
            throw new StationOpenApiDownloadException(
                    "OpenAPI download returned JSON success envelope instead of binary stream",
                    endpointDefinition.path(), requestId, endpointDefinition.operation(), false);
        }
        if (!hasContentType(response, "application/octet-stream")) {
            throw new StationOpenApiDownloadException(
                    "OpenAPI download returned unexpected Content-Type: " + contentTypeDescription(response),
                    endpointDefinition.path(), requestId, endpointDefinition.operation(), false);
        }
        return true;
    }

    private static void copyDownloadToCaller(Path attemptFile, OutputStream outputStream,
            EndpointDefinition endpointDefinition, String requestId) {
        try {
            Files.copy(attemptFile, outputStream);
        } catch (IOException | RuntimeException ex) {
            // 从临时文件复制到调用方输出流时，异常可能发生在客户已经收到部分字节之后。
            // 这时无法由 SDK 判断客户侧目标文件是否完整，必须用 resultUnknown=true 提醒客户复核。
            throw new StationOpenApiDownloadException("Failed to copy OpenAPI download response to caller output",
                    endpointDefinition.path(), requestId, endpointDefinition.operation(), true, ex);
        }
    }

    private static boolean isJsonContentType(HttpResponse response) {
        // application/problem+json 等结构化 JSON 错误也必须进入 R<?> 解析，不能落成客户文件。
        for (Map.Entry<String, List<String>> entry : response.headers().entrySet()) {
            if (!"Content-Type".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (isJsonContentType(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isJsonContentType(String value) {
        if (value == null) {
            return false;
        }
        String mediaType = value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return "application/json".equals(mediaType) || mediaType.endsWith("+json");
    }

    private static boolean hasContentType(HttpResponse response, String expectedMediaType) {
        for (Map.Entry<String, List<String>> entry : response.headers().entrySet()) {
            if (!"Content-Type".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (value != null && expectedMediaType.equalsIgnoreCase(value.split(";", 2)[0].trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String contentTypeDescription(HttpResponse response) {
        for (Map.Entry<String, List<String>> entry : response.headers().entrySet()) {
            if ("Content-Type".equalsIgnoreCase(entry.getKey())) {
                return String.valueOf(entry.getValue());
            }
        }
        return "<missing>";
    }

    private LinkedHashMap<String, String> defaultHeaders(String requestId) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        // 这些 header 是所有 JSON 请求和下载请求的基础值；鉴权 provider 可以追加 token header。
        headers.put("Content-Type", CONTENT_TYPE);
        headers.put("Accept", ACCEPT_JSON);
        headers.put("User-Agent", userAgent);
        headers.put("X-Request-Id", requestId);
        return headers;
    }

    private Duration effectiveReadTimeout(RequestOptions options) {
        // RequestOptions 是单次调用覆盖层；为空时回落到客户端全局默认值。
        return options.readTimeout() == null ? defaultReadTimeout : options.readTimeout();
    }

    private Duration effectiveCallTimeout(RequestOptions options) {
        // 总超时覆盖和读取超时相互独立，便于下载等长读场景单独调大。
        return options.callTimeout() == null ? defaultCallTimeout : options.callTimeout();
    }

    private StationOpenApiTimeoutException timeoutException(EndpointDefinition endpointDefinition, String requestId,
            HttpTimeoutException cause) {
        // resultUnknown 由操作意图决定：读接口可安全告知未知为 false，危险操作必须提醒客户确认状态。
        return new StationOpenApiTimeoutException(failureMessage("OpenAPI request timed out", endpointDefinition),
                endpointDefinition.path(), requestId, endpointDefinition.operation(),
                endpointDefinition.intent().resultUnknownOnConnectionLoss(), cause);
    }

    private StationOpenApiTransportException transportException(EndpointDefinition endpointDefinition, String requestId,
            HttpConnectionException cause) {
        // 连接中断不代表平台一定没收到请求，控制/写入操作按未知结果处理。
        return new StationOpenApiTransportException(
                failureMessage("OpenAPI transport request failed", endpointDefinition), endpointDefinition.path(),
                requestId, endpointDefinition.operation(), endpointDefinition.intent().resultUnknownOnConnectionLoss(),
                cause);
    }

    private StationOpenApiTransportException unexpectedTransportException(
            EndpointDefinition endpointDefinition, String requestId, RuntimeException cause) {
        return new StationOpenApiTransportException(failureMessage(
                "OpenAPI transport raised an unclassified runtime failure", endpointDefinition),
                endpointDefinition.path(), requestId, endpointDefinition.operation(),
                endpointDefinition.intent().resultUnknownOnConnectionLoss(), cause);
    }

    private static String failureMessage(String baseMessage, EndpointDefinition endpointDefinition) {
        if (!endpointDefinition.intent().resultUnknownOnConnectionLoss()) {
            return baseMessage;
        }
        /*
         * 控制/写入请求在断连前可能已经到达平台或设备。异常不仅要给机器可读的 resultUnknown=true，
         * 还要给直接阅读日志的客户明确动作，避免把“不知道结果”误当作“明确失败”立即重发。
         */
        return baseMessage
                + ". The operation may already have taken effect. "
                + "Query status or wait for MQ confirmation before retrying";
    }

    RequestOptions prepareOptions(RequestOptions options) {
        RequestOptions effective = options == null ? RequestOptions.builder().build() : options;
        if (effective.requestId() != null) {
            return effective;
        }
        // 一次公共调用只生成一个 UUID，供 HTTP header、响应后处理和本地文件异常共同复用。
        return RequestOptions.builder()
                .requestId(UUID.randomUUID().toString())
                .readTimeout(effective.readTimeout())
                .callTimeout(effective.callTimeout())
                .retryPolicy(effective.retryPolicy())
                .build();
    }

    private static String sdkVersion() {
        String implementationVersion = OpenApiExecutor.class.getPackage().getImplementationVersion();
        if (implementationVersion == null || implementationVersion.isBlank()) {
            return "1.0.0-SNAPSHOT";
        }
        return implementationVersion;
    }

    /**
     * 下载临时文件抽象，主要用于测试注入失败场景。
     */
    interface DownloadTempFileStore {

        Path create() throws IOException;

        void delete(Path path) throws IOException;
    }

    private static final class DefaultDownloadTempFileStore implements DownloadTempFileStore {
        private final Path directory;

        private DefaultDownloadTempFileStore(Path directory) {
            this.directory = directory;
        }

        @Override
        public Path create() throws IOException {
            if (directory == null) {
                // 每次网络尝试默认写系统临时 .tmp；Path API 外层还会使用目标目录内唯一的随机 .part 文件。
                return Files.createTempFile("station-openapi-download-", ".tmp");
            }
            return Files.createTempFile(directory, "station-openapi-download-", ".tmp");
        }

        @Override
        public void delete(Path path) throws IOException {
            // 允许 path 不存在，下载失败后的 finally 清理不应覆盖原始异常。
            Files.deleteIfExists(path);
        }
    }
}
