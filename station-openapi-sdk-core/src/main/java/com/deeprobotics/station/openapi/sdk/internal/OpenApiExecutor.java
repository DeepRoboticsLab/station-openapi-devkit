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
 * <p>统一处理请求映射、鉴权、JSON 编解码、响应解包、异常、重试和下载。</p>
 *
 * <p>仅供 SDK 内部使用。{@link HttpTransport} 负责单次 I/O，重试和异常转换由本类控制。</p>
 */
public final class OpenApiExecutor {

    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    /** 下载成功时返回二进制流，错误响应仍可能为 JSON。 */
    private static final String ACCEPT_JSON = "application/json";
    private final String endpoint;
    private final AuthProvider authProvider;
    private final HttpTransport transport;
    private final JsonCodec jsonCodec;
    private final Duration defaultReadTimeout;
    private final Duration defaultCallTimeout;
    private final DownloadTempFileStore downloadTempFileStore;
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
     * 使用指定 JSON 编解码器创建执行器，通常使用 {@link JsonCodec#defaultCodec()}。
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
     * <p>每次尝试都重新构造请求并鉴权，避免复用 timestamp/nonce/sign。</p>
     *
     * @param <T> 成功响应中的业务数据类型
     * @param endpointDefinition 接口路径、操作名和安全意图元数据
     * @param request 业务请求模型；无业务字段接口可传 {@code null}
     * @param responseType 完整的 {@code ApiResponse<T>} 泛型类型
     * @param options 请求级覆盖选项；传 {@code null} 时使用客户端默认值
     * @return 解包后的业务数据，可为 {@code null}
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
     * <p>按接口契约选择本入口；任务下发、任务控制和 dogPos 允许空 data，应使用 {@link #execute}。</p>
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
     * <p>每次尝试先写入独立临时文件，完整下载并通过响应校验后才复制给调用方，
     * 避免重试拼接出损坏文件或将 JSON 错误体写入目标流。</p>
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
                    // 每次下载尝试重新鉴权，避免复用 nonce/sign。
                    HttpRequest httpRequest = buildRequest(endpointDef, request, effectiveOptions, requestId);
                    try {
                        response = requireTransportResponse(
                                transport.download(httpRequest, attemptOutput), endpointDef, requestId, "download");
                    } catch (StationOpenApiDownloadException ex) {
                        // 仅临时文件写入失败，尚未写入调用方流，因此 resultUnknown=false。
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
                // 临时文件 I/O 失败，调用方流尚未写入，结果并非未知。
                throw new StationOpenApiDownloadException("Failed to write OpenAPI download response",
                        endpointDef.path(), requestId, endpointDef.operation(), false, ex);
            } finally {
                deleteDownloadTempFile(attemptFile, endpointDef, requestId);
            }
        }
        throw new IllegalStateException("unreachable retry state");
    }

    /**
     * 按业务映射、鉴权、序列化的顺序构造请求，保证签名与发送的请求体一致。
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
            // 未分类异常不重试；统一脱敏，并按操作意图标记 resultUnknown。
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
            // 清理失败不改变下载结果，也不阻断重试。
        }
    }

    /**
     * 校验下载响应。二进制内容已写入临时文件；JSON 失败响应转为业务异常，
     * JSON 成功响应仍属下载协议错误，即使 HTTP 状态为 2xx。
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
            // 调用方流可能已收到部分字节，以 resultUnknown=true 提醒复核目标文件。
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
        headers.put("Content-Type", CONTENT_TYPE);
        headers.put("Accept", ACCEPT_JSON);
        headers.put("User-Agent", userAgent);
        headers.put("X-Request-Id", requestId);
        return headers;
    }

    private Duration effectiveReadTimeout(RequestOptions options) {
        return options.readTimeout() == null ? defaultReadTimeout : options.readTimeout();
    }

    private Duration effectiveCallTimeout(RequestOptions options) {
        return options.callTimeout() == null ? defaultCallTimeout : options.callTimeout();
    }

    private StationOpenApiTimeoutException timeoutException(EndpointDefinition endpointDefinition, String requestId,
            HttpTimeoutException cause) {
        return new StationOpenApiTimeoutException(failureMessage("OpenAPI request timed out", endpointDefinition),
                endpointDefinition.path(), requestId, endpointDefinition.operation(),
                endpointDefinition.intent().resultUnknownOnConnectionLoss(), cause);
    }

    private StationOpenApiTransportException transportException(EndpointDefinition endpointDefinition, String requestId,
            HttpConnectionException cause) {
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
        // 写入/控制可能已生效；除 resultUnknown 标记外，给出确认状态后再重试的指引。
        return baseMessage
                + ". The operation may already have taken effect. "
                + "Query status or wait for MQ confirmation before retrying";
    }

    RequestOptions prepareOptions(RequestOptions options) {
        RequestOptions effective = options == null ? RequestOptions.builder().build() : options;
        if (effective.requestId() != null) {
            return effective;
        }
        // 同一次调用及其重试共用请求 ID，关联 HTTP 请求和本地异常。
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
                // 每次尝试使用系统临时文件；Path API 另用目标目录内的 .part 文件完成最终替换。
                return Files.createTempFile("station-openapi-download-", ".tmp");
            }
            return Files.createTempFile(directory, "station-openapi-download-", ".tmp");
        }

        @Override
        public void delete(Path path) throws IOException {
            Files.deleteIfExists(path);
        }
    }
}
