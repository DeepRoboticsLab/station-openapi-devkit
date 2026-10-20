package com.deeprobotics.station.openapi.sdk.transport.okhttp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClients;
import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.auth.AuthProvider;
import com.deeprobotics.station.openapi.sdk.auth.AuthenticatedRequest;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiBusinessException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiHttpException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTimeoutException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTransportException;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.internal.OpenApiExecutor;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import com.deeprobotics.station.openapi.sdk.model.request.result.ResourceDownloadRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * OkHttp 下载链路测试。
 *
 * <p>下载接口和普通 JSON 接口不同：成功时是二进制流，失败时平台可能返回 JSON 错误体或其他错误页。
 * 这些测试同时覆盖 transport 直连行为和通过 {@link OpenApiExecutor} 后的异常语义，防止后续维护时
 * 把错误体写进客户文件、一次性缓存大文件，或错误设置 {@code resultUnknown}。</p>
 */
class OkHttpDownloadTest {

    /** 与生产代码保持一致的错误体上限，测试非 2xx 大响应不会被完整读入异常对象。 */
    private static final int DOWNLOAD_ERROR_BODY_LIMIT = 64 * 1024;

    private final ArrayList<MockWebServer> servers = new ArrayList<>();

    @AfterEach
    void shutdownServers() throws IOException {
        // 下载测试会创建多个 MockWebServer；即使断言失败也要释放端口和后台线程。
        for (MockWebServer server : servers) {
            server.shutdown();
        }
    }

    @Test
    void downloadStreamsBytesWithoutBufferingEntireFileInSdkCode() throws Exception {
        // 混合大小写且带参数的媒体类型应被识别；大响应体和 guard 流同时验证 transport 始终分块写出。
        byte[] fileBytes = asciiBytes(128 * 1024 + 3);
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "Application/Octet-Stream; charset=binary")
                .setBody(new String(fileBytes, StandardCharsets.US_ASCII)));
        StreamingGuardOutputStream output = new StreamingGuardOutputStream(fileBytes.length);

        try (StationOpenApiClient client = newClient(server)) {
            client.result().downloadResource(downloadRequest(), output);

            assertThat(output.toByteArray()).isEqualTo(fileBytes);
            assertThat(output.maxWriteLength()).isLessThanOrEqualTo(8192);
            assertThat(output.writeCalls()).isGreaterThan(1);
        }
        assertThat(output.closed).isFalse();
    }

    @Test
    void progressingDownloadOutlastsGlobalCallAndReadTimeouts() throws Exception {
        byte[] fileBytes = asciiBytes(12 * 1024);
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/octet-stream")
                .setBody(new String(fileBytes, StandardCharsets.US_ASCII))
                .throttleBody(1024, 150, TimeUnit.MILLISECONDS));
        StreamingGuardOutputStream output = new StreamingGuardOutputStream(fileBytes.length);

        try (StationOpenApiClient client = StationOpenApiClients.builder()
                .endpoint(server.url("/").toString())
                .accessToken("download-test-token")
                .readTimeout(Duration.ofSeconds(1))
                .callTimeout(Duration.ofMillis(300))
                .build()) {
            client.result().downloadResource(downloadRequest(), output);
        }

        assertThat(output.toByteArray()).isEqualTo(fileBytes);
        assertThat(output.maxWriteLength()).isLessThanOrEqualTo(8192);
        assertThat(output.closed).isFalse();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    @Timeout(30)
    void defaultDownloadCanStreamForLongerThanFifteenSeconds() throws Exception {
        byte[] fileBytes = asciiBytes(18 * 1024);
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/octet-stream")
                .setBody(new String(fileBytes, StandardCharsets.US_ASCII))
                .throttleBody(1024, 1, TimeUnit.SECONDS));
        StreamingGuardOutputStream output = new StreamingGuardOutputStream(fileBytes.length);

        long started = System.nanoTime();
        try (StationOpenApiClient client = StationOpenApiClients.builder()
                .endpoint(server.url("/").toString())
                .accessToken("download-test-token")
                .build()) {
            client.result().downloadResource(downloadRequest(), output);
        }

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isGreaterThan(Duration.ofSeconds(15));
        assertThat(output.toByteArray()).isEqualTo(fileBytes);
        assertThat(output.closed).isFalse();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void explicitDownloadCallTimeoutStopsProgressingTransferWithoutRetry() throws Exception {
        byte[] fileBytes = asciiBytes(20 * 1024);
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/octet-stream")
                .setBody(new String(fileBytes, StandardCharsets.US_ASCII))
                .throttleBody(1024, 150, TimeUnit.MILLISECONDS));
        StreamingGuardOutputStream output = new StreamingGuardOutputStream(fileBytes.length);

        try (StationOpenApiClient client = newClient(server)) {
            assertThatThrownBy(() -> client.result().downloadResource(downloadRequest(), output,
                    RequestOptions.builder().readTimeout(Duration.ofSeconds(2))
                            .callTimeout(Duration.ofMillis(500)).build()))
                    .isInstanceOf(StationOpenApiTimeoutException.class)
                    .satisfies(thrown -> assertThat(((StationOpenApiTimeoutException) thrown).resultUnknown())
                            .isTrue());
        }

        assertThat(output.toByteArray().length).isBetween(1, fileBytes.length - 1);
        assertThat(output.closed).isFalse();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void downloadWithoutTotalTimeoutStillFailsOnReadStallWithoutRetry() throws Exception {
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/octet-stream")
                .setBody("delayed-file")
                .setBodyDelay(1, TimeUnit.SECONDS));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (StationOpenApiClient client = newClient(server)) {
            assertThatThrownBy(() -> client.result().downloadResource(downloadRequest(), output,
                    RequestOptions.builder().readTimeout(Duration.ofMillis(100)).build()))
                    .isInstanceOf(StationOpenApiTimeoutException.class)
                    .satisfies(thrown -> assertThat(((StationOpenApiTimeoutException) thrown).resultUnknown())
                            .isTrue());
        }

        assertThat(output.size()).isZero();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void directDownloadDefaultsToTenSecondReadsAndNoTotalTimeout() {
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/octet-stream").setBody("file"));
        OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(chain -> {
            assertThat(chain.readTimeoutMillis()).isEqualTo(10_000);
            assertThat(chain.call().timeout().timeoutNanos()).isZero();
            return chain.proceed(chain.request());
        }).build();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (OkHttpTransport transport = OkHttpTransportFactory.create(httpClient)) {
            transport.download(new HttpRequest(server.url("/remoteApi/result/resource").uri(),
                    Map.of(), "{}".getBytes(StandardCharsets.UTF_8), null, null), output);
            assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("file");
        } finally {
            httpClient.dispatcher().executorService().shutdownNow();
            httpClient.connectionPool().evictAll();
        }
    }

    @Test
    void interruptedDownloadExposesPartialOutputAndDoesNotRetry() throws Exception {
        byte[] fileBytes = asciiBytes(128 * 1024);
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/octet-stream")
                .setBody(new String(fileBytes, StandardCharsets.US_ASCII))
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/octet-stream")
                .setBody("retried-download"));
        StreamingGuardOutputStream output = new StreamingGuardOutputStream(fileBytes.length);

        try (StationOpenApiClient client = newClient(server)) {
            assertThatThrownBy(() -> client.result().downloadResource(downloadRequest(), output,
                    RequestOptions.builder().requestId("interrupted-download").build()))
                    .isInstanceOf(StationOpenApiTransportException.class)
                    .hasMessageContaining("interrupted-download")
                    .hasMessageContaining("/remoteApi/result/resource")
                    .hasMessageContaining("result.downloadResource")
                    .satisfies(thrown -> assertThat(((StationOpenApiTransportException) thrown).resultUnknown())
                            .isTrue());
        }
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(output.toByteArray().length).isBetween(1, fileBytes.length - 1);
        assertThat(output.toByteArray()).isEqualTo(slice(fileBytes, output.toByteArray().length));
        assertThat(output.closed).isFalse();
    }

    @Test
    void jsonDownloadFailureIsParsedAsBusinessExceptionAndNotWrittenToCallerOutput() throws Exception {
        // 平台下载失败可能仍返回 HTTP 2xx + JSON R 失败包；executor 应按业务失败处理，而不是生成 JSON 文件。
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("{\"success\":false,\"code\":40003,\"msg\":\"download permission denied\","
                        + "\"data\":null}"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (OkHttpTransport transport = OkHttpTransportFactory.create()) {
            OpenApiExecutor executor = newExecutor(server, transport);

            assertThatThrownBy(() -> executor.download(downloadEndpoint(), new StaticRequest("filePath", "map.bin",
                    "type", 1), output, RequestOptions.builder().requestId("download-json-error").build()))
                    .isInstanceOf(StationOpenApiBusinessException.class)
                    .hasMessageContaining("code=40003")
                    .hasMessageContaining("download permission denied")
                    .hasMessageContaining("/remoteApi/result/resource");
        }
        assertThat(output.size()).isZero();
    }

    @Test
    void problemJsonDownloadFailureKeepsBodyForBusinessErrorParsing() throws Exception {
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/problem+json")
                .setBody("{\"success\":false,\"code\":40003,\"msg\":\"download denied\",\"data\":null}"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (OkHttpTransport transport = OkHttpTransportFactory.create()) {
            OpenApiExecutor executor = newExecutor(server, transport);

            assertThatThrownBy(() -> executor.download(downloadEndpoint(), new StaticRequest("filePath", "map.bin",
                    "type", 1), output, RequestOptions.builder().requestId("download-problem-json").build()))
                    .isInstanceOf(StationOpenApiBusinessException.class)
                    .hasMessageContaining("download denied");
        }
        assertThat(output.size()).isZero();
    }

    @Test
    void successfulHtmlResponseIsDiagnosticBodyAndNeverWrittenAsFile() throws Exception {
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("<html>gateway login</html>"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (OkHttpTransport transport = OkHttpTransportFactory.create()) {
            HttpResponse response = transport.download(new HttpRequest(
                    server.url("/remoteApi/result/resource").uri(),
                    Map.of("Content-Type", "application/json; charset=utf-8"),
                    "{\"filePath\":\"map.bin\",\"type\":1}".getBytes(StandardCharsets.UTF_8),
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(10)), output);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(new String(response.body(), StandardCharsets.UTF_8)).contains("gateway login");
            assertThat(output.size()).isZero();
        }
    }

    @Test
    void non2xxDownloadResponseThrowsHttpExceptionAndDoesNotWriteCallerOutput() throws Exception {
        // 非 2xx 响应保留状态码，不泄露原始错误体，也不写入调用方输出流。
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody("{\"success\":false,\"code\":50300,\"msg\":\"download server unavailable\","
                        + "\"data\":null}"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (OkHttpTransport transport = OkHttpTransportFactory.create()) {
            OpenApiExecutor executor = newExecutor(server, transport);

            assertThatThrownBy(() -> executor.download(downloadEndpoint(), new StaticRequest("filePath", "map.bin",
                    "type", 1), output, RequestOptions.builder().requestId("download-http-error").build()))
                    .isInstanceOf(StationOpenApiHttpException.class)
                    .hasMessageContaining("code=503")
                    .hasMessageNotContaining("download server unavailable")
                    .hasMessageContaining("/remoteApi/result/resource");
        }
        assertThat(output.size()).isZero();
    }

    @Test
    void downloadMarksCallerOutputWriteFailureAsResultUnknown() throws Exception {
        byte[] fileBytes = asciiBytes(32 * 1024);
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/octet-stream")
                .setBody(new String(fileBytes, StandardCharsets.US_ASCII)));
        FailingAfterPartialWriteOutputStream output = new FailingAfterPartialWriteOutputStream(100);

        try (StationOpenApiClient client = newClient(server)) {
            assertThatThrownBy(() -> client.result().downloadResource(downloadRequest(), output,
                    RequestOptions.builder().requestId("output-failure").build()))
                    .isInstanceOf(StationOpenApiDownloadException.class)
                    .satisfies(thrown -> assertThat(((StationOpenApiDownloadException) thrown).resultUnknown())
                            .isTrue())
                    .hasMessageContaining("output-failure")
                    .hasMessageContaining("result.downloadResource")
                    .hasMessageContaining("/remoteApi/result/resource");
        }
        assertThat(output.bytesWritten()).isPositive();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void downloadWrapsUncheckedCallerOutputFailureAndRedactsCause() throws Exception {
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/octet-stream")
                .setBody("complete-download"));
        RuntimeFailingOutputStream output = new RuntimeFailingOutputStream();

        try (StationOpenApiClient client = newClient(server)) {
            assertThatThrownBy(() -> client.result().downloadResource(downloadRequest(), output,
                    RequestOptions.builder().requestId("runtime-output-failure").build()))
                    .isInstanceOf(StationOpenApiDownloadException.class)
                    .hasMessageContaining("runtime-output-failure")
                    .hasMessageContaining("result.downloadResource")
                    .hasMessageNotContaining("raw-output-password")
                    .satisfies(thrown -> assertThat(((StationOpenApiDownloadException) thrown).resultUnknown())
                            .isTrue());
        }
        assertThat(output.writeAttempted()).isTrue();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void directDownloadLargeNon2xxOctetStreamErrorBodyIsCappedAndNotWrittenToCallerOutput() throws Exception {
        // 某些网关会用二进制或 HTML 返回大错误页；transport 只能截取诊断片段，不能把它写进目标文件。
        byte[] errorBytes = asciiBytes(DOWNLOAD_ERROR_BODY_LIMIT + 4096);
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/octet-stream")
                .setBody(new String(errorBytes, StandardCharsets.US_ASCII)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (OkHttpTransport transport = OkHttpTransportFactory.create()) {
            HttpResponse response = transport.download(new HttpRequest(
                    server.url("/remoteApi/result/resource").uri(),
                    Map.of("Content-Type", "application/json; charset=utf-8"),
                    "{\"filePath\":\"map.bin\",\"type\":1}".getBytes(StandardCharsets.UTF_8),
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(10)), output);

            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.body()).hasSize(DOWNLOAD_ERROR_BODY_LIMIT);
            assertThat(response.body()).isEqualTo(slice(errorBytes, DOWNLOAD_ERROR_BODY_LIMIT));
            assertThat(output.size()).isZero();
        }
    }

    private StationOpenApiClient newClient(MockWebServer server) {
        return StationOpenApiClients.builder()
                .endpoint(server.url("/web").toString())
                .accessToken("download-test-token")
                .build();
    }

    private static ResourceDownloadRequest downloadRequest() {
        return ResourceDownloadRequest.builder().filePath("map.bin").type(1).build();
    }

    private OpenApiExecutor newExecutor(MockWebServer server, OkHttpTransport transport) {
        // 使用透传鉴权 provider，让测试聚焦下载响应处理，而不是重复覆盖签名/token 逻辑。
        StationOpenApiConfig config = StationOpenApiConfig.builder()
                .endpoint(server.url("/web").toString())
                .accessToken("download-test-token")
                .build();
        return new OpenApiExecutor(config, new PassthroughAuthProvider(), transport);
    }

    private static EndpointDefinition downloadEndpoint() {
        return EndpointDefinition.of("result.downloadResource", "/remoteApi/result/resource",
                OperationIntent.FILE_DOWNLOAD);
    }

    private MockWebServer newServer() {
        // 统一登记，避免每个测试手写关闭逻辑。
        MockWebServer server = new MockWebServer();
        servers.add(server);
        return server;
    }

    private static byte[] asciiBytes(int length) {
        // 用可预测 ASCII 内容构造大文件/大错误体，便于精确比较截断结果。
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ('a' + (i % 26));
        }
        return bytes;
    }

    private static byte[] slice(byte[] bytes, int length) {
        // 避免引入额外依赖，只取前 length 字节用于验证错误体截断。
        byte[] copy = new byte[length];
        System.arraycopy(bytes, 0, copy, 0, length);
        return copy;
    }

    private static final class PassthroughAuthProvider implements AuthProvider {
        @Override
        public AuthenticatedRequest authenticate(Map<String, Object> businessBody) {
            // 测试 transport/executor 下载语义时不需要真实鉴权，保持 body 原样便于断言。
            return new AuthenticatedRequest(businessBody, Map.of());
        }
    }

    private static final class StaticRequest implements OpenApiRequest {
        private final LinkedHashMap<String, Object> body = MapUtils.newBody();

        private StaticRequest(Object... keyValues) {
            // 测试用轻量请求对象，保持插入顺序，模拟生产 request.toMap() 的确定性输出。
            for (int i = 0; i < keyValues.length; i += 2) {
                body.put((String) keyValues[i], keyValues[i + 1]);
            }
        }

        @Override
        public LinkedHashMap<String, Object> toMap() {
            // 和生产请求模型一样过滤 null，避免测试请求与真实请求映射规则脱节。
            return MapUtils.copyWithoutNulls(body);
        }
    }

    private static final class StreamingGuardOutputStream extends OutputStream {
        private final int fullLength;
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private int writeCalls;
        private int maxWriteLength;
        private boolean closed;

        private StreamingGuardOutputStream(int fullLength) {
            this.fullLength = fullLength;
        }

        @Override
        public void write(int value) {
            delegate.write(value);
            writeCalls++;
            maxWriteLength = Math.max(maxWriteLength, 1);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            // 如果 transport 一次写入完整文件，说明下载路径可能已经把整个响应缓存到内存。
            if (length >= fullLength) {
                throw new AssertionError("download wrote the entire file in one buffered chunk");
            }
            delegate.write(bytes, offset, length);
            writeCalls++;
            maxWriteLength = Math.max(maxWriteLength, length);
        }

        private byte[] toByteArray() {
            return delegate.toByteArray();
        }

        @Override
        public void close() {
            closed = true;
        }

        private int writeCalls() {
            return writeCalls;
        }

        private int maxWriteLength() {
            return maxWriteLength;
        }
    }

    private static final class FailingAfterPartialWriteOutputStream extends OutputStream {
        private final int failAfterBytes;
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();

        private FailingAfterPartialWriteOutputStream(int failAfterBytes) {
            this.failAfterBytes = failAfterBytes;
        }

        @Override
        public void write(int value) throws IOException {
            // 模拟客户磁盘、网络文件系统或外部流在已经接收部分字节后失败。
            if (delegate.size() >= failAfterBytes) {
                throw new IOException("caller output failed after partial write");
            }
            delegate.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            // 先写入允许的部分再抛错，逼近真实“半截文件”场景。
            int allowed = Math.max(0, failAfterBytes - delegate.size());
            if (allowed > 0) {
                delegate.write(bytes, offset, Math.min(allowed, length));
            }
            throw new IOException("caller output failed after partial write");
        }

        private int bytesWritten() {
            return delegate.size();
        }
    }

    private static final class RuntimeFailingOutputStream extends OutputStream {
        private boolean writeAttempted;

        @Override
        public void write(int value) {
            writeAttempted = true;
            throw new IllegalStateException("password=raw-output-password");
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            writeAttempted = true;
            throw new IllegalStateException("password=raw-output-password");
        }

        private boolean writeAttempted() {
            return writeAttempted;
        }
    }
}
