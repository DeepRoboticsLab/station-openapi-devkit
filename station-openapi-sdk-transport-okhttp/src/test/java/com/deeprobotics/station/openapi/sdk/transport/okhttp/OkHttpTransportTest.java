package com.deeprobotics.station.openapi.sdk.transport.okhttp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClients;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpConnectionException;
import com.deeprobotics.station.openapi.sdk.http.HttpTimeoutException;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogChargeControlRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * OkHttp transport 的基础 HTTP 行为测试。
 *
 * <p>这些测试只验证 transport 边界：请求是否按 core 给定内容发送、响应字节是否原样保留、
 * 单次超时是否生效，以及默认 OkHttpClient 是否符合 SDK 的安全策略。鉴权、签名、重试和
 * {@code R<T>} 解包由 core 测试覆盖。</p>
 */
class OkHttpTransportTest {

    private final ArrayList<MockWebServer> servers = new ArrayList<>();

    @Test
    void transportCreationIsFactoryOnly() {
        assertThat(OkHttpTransport.class.getConstructors()).isEmpty();
    }

    @AfterEach
    void shutdownServers() throws IOException {
        // 每个测试可能创建独立 MockWebServer；统一关闭，避免端口和后台线程影响后续测试。
        for (MockWebServer server : servers) {
            server.shutdown();
        }
    }

    @Test
    void postJsonSendsExactBodyAndPreservesResponseBytes() throws Exception {
        // transport 不应重新序列化、补签或改写 body。core 传入什么 JSON 字节，服务端就应收到什么。
        byte[] requestBody = "{\"dogCode\":\"DOG-001\"}".getBytes(StandardCharsets.UTF_8);
        byte[] responseBody = "{\"success\":true,\"code\":200,\"msg\":\"OK\",\"data\":\"done\"}"
                .getBytes(StandardCharsets.UTF_8);
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(new String(responseBody, StandardCharsets.UTF_8)));

        try (OkHttpTransport transport = OkHttpTransportFactory.create()) {
            HttpResponse response = transport.postJson(new HttpRequest(
                    server.url("/remoteApi/time").uri(),
                    Map.of("Content-Type", "application/json; charset=utf-8", "X-Request-Id", "request-post"),
                    requestBody,
                    Duration.ofSeconds(3),
                    Duration.ofSeconds(5)));

            RecordedRequest recorded = server.takeRequest(1, TimeUnit.SECONDS);
            assertThat(recorded).isNotNull();
            assertThat(recorded.getMethod()).isEqualTo("POST");
            assertThat(recorded.getPath()).isEqualTo("/remoteApi/time");
            assertThat(recorded.getBody().readByteArray()).isEqualTo(requestBody);
            assertThat(recorded.getHeader("X-Request-Id")).isEqualTo("request-post");
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo(responseBody);
            assertThat(response.headers().keySet())
                    .anySatisfy(header -> assertThat(header).isEqualToIgnoringCase("Content-Type"));
        }
    }

    @Test
    void readTimeoutCanBeAppliedPerRequest() throws Exception {
        // 单次 readTimeout 用来支持客户对慢查询或下载前置接口做局部调优，不应只能依赖全局 client。
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("late-body")
                .setBodyDelay(1, TimeUnit.SECONDS));

        try (OkHttpTransport transport = OkHttpTransportFactory.create()) {
            HttpRequest request = new HttpRequest(
                    server.url("/remoteApi/time").uri(),
                    Map.of("Content-Type", "application/json; charset=utf-8"),
                    "{}".getBytes(StandardCharsets.UTF_8),
                    Duration.ofMillis(100),
                    Duration.ofSeconds(3));

            assertThatThrownBy(() -> transport.postJson(request))
                    .isInstanceOf(HttpTimeoutException.class);
        }
    }

    @Test
    void callTimeoutCanBeAppliedPerRequest() throws Exception {
        // callTimeout 覆盖整个 call 生命周期；这里延迟响应头，验证超时不是只发生在 body 读取阶段。
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok")
                .setHeadersDelay(1, TimeUnit.SECONDS));

        try (OkHttpTransport transport = OkHttpTransportFactory.create()) {
            HttpRequest request = new HttpRequest(
                    server.url("/remoteApi/time").uri(),
                    Map.of("Content-Type", "application/json; charset=utf-8"),
                    "{}".getBytes(StandardCharsets.UTF_8),
                    Duration.ofSeconds(3),
                    Duration.ofMillis(100));

            assertThatThrownBy(() -> transport.postJson(request))
                    .isInstanceOf(HttpTimeoutException.class);
        }
    }

    @Test
    void defaultClientDisablesOkHttpRetriesAndRedirects() {
        // 自动重试和重定向都可能绕过 core 的操作意图判断，尤其会影响控制/写入类请求的安全边界。
        OkHttpClient client = OkHttpTransportFactory.newDefaultClient();
        try {
            assertThat(client.retryOnConnectionFailure()).isFalse();
            assertThat(client.followRedirects()).isFalse();
            assertThat(client.followSslRedirects()).isFalse();
            assertThat(client.dispatcher().getMaxRequests()).isEqualTo(64);
            assertThat(client.dispatcher().getMaxRequestsPerHost()).isEqualTo(5);
        } finally {
            client.dispatcher().executorService().shutdownNow();
            client.connectionPool().evictAll();
        }
    }

    @Test
    void defaultClientUsesConfiguredConnectTimeout() {
        OkHttpClient client = OkHttpTransportFactory.newDefaultClient(Duration.ofMillis(1234));
        try {
            assertThat(client.connectTimeoutMillis()).isEqualTo(1234);
        } finally {
            client.dispatcher().executorService().shutdownNow();
            client.connectionPool().evictAll();
        }
    }

    @Test
    void oneSdkAttemptDoesNotFollowRetryAfterZero503() throws Exception {
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(503)
                .setHeader("Retry-After", "0")
                .setBody("{\"success\":false,\"code\":50300,\"msg\":\"busy\"}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("unexpected-second-response"));

        try (OkHttpTransport transport = OkHttpTransportFactory.create()) {
            HttpResponse response = transport.postJson(request(server, "/remoteApi/controlDog",
                    Map.of("X-Access-Token", "sensitive-token")));

            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(server.getRequestCount()).isEqualTo(1);
        }
    }

    @Test
    void nonSuccessfulJsonResponseBodyIsLimitedTo64KiB() {
        int limit = 64 * 1024;
        MockWebServer server = newServer();
        server.enqueue(new MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("x".repeat(limit + 4096)));

        try (OkHttpTransport transport = OkHttpTransportFactory.create()) {
            HttpResponse response = transport.postJson(
                    request(server, "/remoteApi/time", Map.of()));

            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.body()).hasSize(limit).containsOnly((byte) 'x');
            assertThat(server.getRequestCount()).isEqualTo(1);
        }
    }

    @Test
    void customerClientRedirectPolicyIsHardenedAndTokenNeverLeavesOriginalOrigin() throws Exception {
        MockWebServer source = newServer();
        MockWebServer target = newServer();
        source.enqueue(new MockResponse()
                .setResponseCode(307)
                .setHeader("Location", target.url("/credential-collector"))
                .setBody("redirect blocked"));
        target.enqueue(new MockResponse().setResponseCode(200).setBody("must-not-be-reached"));
        OkHttpClient customerClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        try (OkHttpTransport transport = OkHttpTransportFactory.create(customerClient)) {
            HttpResponse response = transport.postJson(request(source, "/remoteApi/time",
                    Map.of("X-Access-Token", "sensitive-token")));

            assertThat(response.statusCode()).isEqualTo(307);
            assertThat(source.getRequestCount()).isEqualTo(1);
            assertThat(target.getRequestCount()).isZero();
        } finally {
            customerClient.dispatcher().executorService().shutdownNow();
            customerClient.connectionPool().evictAll();
        }
    }

    @Test
    void customerRetryInterceptorCannotProceedTwiceWithinOneSdkAttempt() throws Exception {
        MockWebServer server = newServer();
        server.enqueue(new MockResponse().setResponseCode(503).setBody("first-response"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("second-response"));
        OkHttpClient customerClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    try (okhttp3.Response ignored = chain.proceed(chain.request())) {
                        // 模拟客户通用网络栈中的透明重试拦截器：丢弃第一次响应并再次发送同一 POST。
                    }
                    return chain.proceed(chain.request());
                })
                .build();

        try (OkHttpTransport transport = OkHttpTransportFactory.create(customerClient)) {
            assertThatThrownBy(() -> transport.postJson(request(server, "/remoteApi/controlDog", Map.of())))
                    .isInstanceOf(HttpConnectionException.class);
            assertThat(server.getRequestCount()).isEqualTo(1);
        } finally {
            customerClient.dispatcher().executorService().shutdownNow();
            customerClient.connectionPool().evictAll();
        }
    }

    @Test
    void factoryRejectsInvalidInputsWithSdkClientException() {
        Duration tooLarge = Duration.ofMillis((long) Integer.MAX_VALUE + 1L);
        Duration overflowing = Duration.ofSeconds(Long.MAX_VALUE);

        for (Duration invalid : new Duration[] {null, Duration.ZERO, Duration.ofNanos(-1), tooLarge, overflowing}) {
            assertThatThrownBy(() -> OkHttpTransportFactory.newDefaultClient(invalid))
                    .as("invalid connect timeout: %s", invalid)
                    .isExactlyInstanceOf(StationOpenApiClientException.class)
                    .hasMessageContaining("connectTimeout")
                    .satisfies(failure -> assertThat(
                            ((StationOpenApiClientException) failure).resultUnknown()).isFalse());
        }

        assertThatThrownBy(() -> OkHttpTransportFactory.create((OkHttpClient) null))
                .isExactlyInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("client");
    }

    @Test
    void oversizedRequestTimeoutFailsLocallyWithoutUnknownResultOrNetworkIo() {
        MockWebServer server = newServer();
        Duration tooLarge = Duration.ofMillis((long) Integer.MAX_VALUE + 1L);

        try (OkHttpTransport transport = OkHttpTransportFactory.create();
                StationOpenApiClient client = StationOpenApiClients.builder()
                        .endpoint(server.url("/").toString())
                        .accessToken("token-value")
                        .transport(transport)
                        .build()) {
            DogChargeControlRequest request = DogChargeControlRequest.builder()
                    .dogCode("DOG-001")
                    .command(DogChargeControlRequest.Command.RETURN_TO_CHARGE)
                    .build();
            RequestOptions options = RequestOptions.builder()
                    .readTimeout(tooLarge)
                    .build();

            assertThatThrownBy(() -> client.dog().chargeControl(request, options))
                    .isInstanceOfSatisfying(StationOpenApiClientException.class, failure -> {
                        assertThat(failure.getMessage()).contains("readTimeout");
                        assertThat(failure.resultUnknown()).isFalse();
                    });
            assertThat(server.getRequestCount()).isZero();
        }
    }

    @Test
    void customerProvidedOkHttpClientIsNotClosedByDefault() {
        // 客户注入的 client 可能被多个业务复用，SDK close 不能替客户关闭共享连接池。
        OkHttpClient client = new OkHttpClient.Builder().build();
        client.dispatcher().executorService();

        OkHttpTransport transport = OkHttpTransportFactory.create(client);
        transport.close();

        assertThat(client.dispatcher().executorService().isShutdown()).isFalse();
        client.dispatcher().executorService().shutdownNow();
        client.connectionPool().evictAll();
    }

    private static HttpRequest request(MockWebServer server, String path, Map<String, String> headers) {
        return new HttpRequest(
                server.url(path).uri(),
                headers,
                "{}".getBytes(StandardCharsets.UTF_8),
                Duration.ofSeconds(3),
                Duration.ofSeconds(5));
    }

    private MockWebServer newServer() {
        // MockWebServer 默认懒启动，首次 url() 调用时绑定端口；记录实例便于 AfterEach 清理。
        MockWebServer server = new MockWebServer();
        servers.add(server);
        return server;
    }
}
