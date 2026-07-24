package com.deeprobotics.station.openapi.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.RequestOptions.RetryPolicy;
import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig.AuthMode;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportProvider;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportSettings;
import com.deeprobotics.station.openapi.sdk.internal.DefaultCameraApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultDogApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultInventoryApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultResultApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultSystemApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultTaskApi;
import com.deeprobotics.station.openapi.sdk.internal.DefaultTaskTemplateApi;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 客户端配置、SPI 自动装配和资源所有权测试。
 *
 * <p>覆盖 endpoint 归一化、两种鉴权互斥、默认/请求级超时、显式 transport 与自动发现 transport 的
 * 生命周期差异，以及 provider 加载失败时的脱敏异常。测试只使用内存 transport 和临时 SPI 文件，
 * 不连接真实平台。</p>
 */
class StationOpenApiClientBuilderTest {

    @Test
    void endpointIsRequired() {
        assertThatThrownBy(() -> signatureConfigBuilder().endpoint(null).build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("endpoint");

        assertThatThrownBy(() -> signatureConfigBuilder().endpoint(" ").build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("endpoint");
    }

    @Test
    void endpointTrailingSlashIsNormalized() {
        StationOpenApiConfig config = signatureConfigBuilder()
                .endpoint("http://127.0.0.1:23288/")
                .build();

        assertThat(config.endpoint()).isEqualTo("http://127.0.0.1:23288");
    }

    @Test
    void endpointMayIncludeContextPath() {
        StationOpenApiConfig config = tokenConfigBuilder()
                .endpoint("http://host:23282/web/")
                .build();

        assertThat(config.endpoint()).isEqualTo("http://host:23282/web");
    }

    @Test
    void endpointRejectsUnsupportedOrCredentialBearingUrisDuringBuild() {
        assertEndpointRejected("ftp://station.example.com/web", "http or https");
        assertEndpointRejected("https://station.example.com/web?tenant=1", "query");
        assertEndpointRejected("https://station.example.com/web#fragment", "fragment");
        assertEndpointRejected("https://customer:uri-password@station.example.com/web", "user-info");
        assertEndpointRejected("https://station.example.com/%", "invalid");
    }

    @Test
    void endpointAcceptsHttpAndHttpsContextPaths() {
        assertThat(tokenConfigBuilder().endpoint("HTTP://station.example.com/web/").build().endpoint())
                .isEqualTo("HTTP://station.example.com/web");
        assertThat(tokenConfigBuilder().endpoint("https://station.example.com/context/api/").build().endpoint())
                .isEqualTo("https://station.example.com/context/api");
    }

    @Test
    void exactlyOneAuthModeIsConfigured() {
        assertThatThrownBy(() -> StationOpenApiConfig.builder()
                .endpoint("http://127.0.0.1:23288")
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("auth");
    }

    @Test
    void appKeyAndSecretKeySelectSignatureMode() {
        StationOpenApiConfig config = signatureConfigBuilder().build();

        assertThat(config.authMode()).isEqualTo(AuthMode.SIGNATURE);
        assertThat(config.appKey()).isEqualTo("app");
    }

    @Test
    void accessTokenSelectsTokenMode() {
        StationOpenApiConfig config = tokenConfigBuilder().build();

        assertThat(config.authMode()).isEqualTo(AuthMode.ACCESS_TOKEN);
    }

    @Test
    void partialOrMixedAuthConfigurationFails() {
        assertThatThrownBy(() -> StationOpenApiConfig.builder()
                .endpoint("http://127.0.0.1:23288")
                .appKey("app")
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("secretKey");

        assertThatThrownBy(() -> StationOpenApiConfig.builder()
                .endpoint("http://127.0.0.1:23288")
                .secretKey("secret")
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("appKey");

        assertThatThrownBy(() -> StationOpenApiConfig.builder()
                .endpoint("http://127.0.0.1:23288")
                .appKey("app")
                .secretKey("secret")
                .accessToken("token")
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("exactly one");
    }

    @Test
    void defaultTimeoutsAreConfigured() {
        StationOpenApiConfig config = signatureConfigBuilder().build();

        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(config.readTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.callTimeout()).isEqualTo(Duration.ofSeconds(15));
    }

    @Test
    void globalTimeoutsAndExplicitTransportRejectInvalidValuesWithSdkException() {
        assertThatThrownBy(() -> tokenConfigBuilder().connectTimeout(null).build())
                .isExactlyInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("connectTimeout");
        assertThatThrownBy(() -> tokenConfigBuilder().readTimeout(Duration.ZERO).build())
                .isExactlyInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("readTimeout");
        assertThatThrownBy(() -> tokenConfigBuilder().callTimeout(Duration.ofSeconds(-1)).build())
                .isExactlyInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("callTimeout");
        assertThatThrownBy(() -> StationOpenApiClients.builder().transport(null))
                .isExactlyInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("transport");
    }

    @Test
    void requestOptionsCanOverrideRequestScopedValues() {
        RequestOptions options = RequestOptions.builder()
                .requestId("req-1")
                .readTimeout(Duration.ofSeconds(30))
                .callTimeout(Duration.ofSeconds(40))
                .retryPolicy(RetryPolicy.NEVER)
                .build();

        assertThat(options.requestId()).isEqualTo("req-1");
        assertThat(options.readTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(options.callTimeout()).isEqualTo(Duration.ofSeconds(40));
        assertThat(options.retryPolicy()).isEqualTo(RetryPolicy.NEVER);
    }

    @Test
    void requestOptionsDefaultsUseClientTimeoutsAndDefaultRetryPolicy() {
        RequestOptions options = RequestOptions.builder().build();

        assertThat(options.readTimeout()).isNull();
        assertThat(options.callTimeout()).isNull();
        assertThat(options.retryPolicy()).isEqualTo(RetryPolicy.DEFAULT);
    }

    @Test
    void requestOptionsRejectInvalidReadTimeout() {
        assertThatThrownBy(() -> RequestOptions.builder()
                .readTimeout(Duration.ZERO)
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("readTimeout");

        assertThatThrownBy(() -> RequestOptions.builder()
                .readTimeout(Duration.ofSeconds(-1))
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("readTimeout");
    }

    @Test
    void requestOptionsRejectInvalidCallTimeout() {
        assertThatThrownBy(() -> RequestOptions.builder()
                .callTimeout(Duration.ZERO)
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("callTimeout");

        assertThatThrownBy(() -> RequestOptions.builder()
                .callTimeout(Duration.ofSeconds(-1))
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("callTimeout");
    }

    @Test
    void requestOptionsRejectNullRetryPolicy() {
        assertThatThrownBy(() -> RequestOptions.builder()
                .retryPolicy(null)
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("retryPolicy");
    }

    @Test
    void explicitTransportBuildsExecutableApiGroupsWithoutTransferringOwnership() throws Exception {
        RecordingTransport transport = new RecordingTransport();
        StationOpenApiClient client = StationOpenApiClients.builder()
                .endpoint("http://127.0.0.1:23288")
                .accessToken("token-value")
                .transport(transport)
                .build();

        assertThat(client).isInstanceOf(AutoCloseable.class);
        assertThat(client.inventory()).isInstanceOf(DefaultInventoryApi.class);
        assertThat(client.dog()).isInstanceOf(DefaultDogApi.class);
        assertThat(client.camera()).isInstanceOf(DefaultCameraApi.class);
        assertThat(client.taskTemplate()).isInstanceOf(DefaultTaskTemplateApi.class);
        assertThat(client.task()).isInstanceOf(DefaultTaskApi.class);
        assertThat(client.result()).isInstanceOf(DefaultResultApi.class);
        assertThat(client.system()).isInstanceOf(DefaultSystemApi.class);
        Object executor = executorOf(client.inventory());
        assertThat(executorOf(client.dog())).isSameAs(executor);
        assertThat(executorOf(client.camera())).isSameAs(executor);
        assertThat(executorOf(client.taskTemplate())).isSameAs(executor);
        assertThat(executorOf(client.task())).isSameAs(executor);
        assertThat(executorOf(client.result())).isSameAs(executor);
        assertThat(executorOf(client.system())).isSameAs(executor);

        assertThat(client.system().getTimeText()).isEqualTo("2026-07-10T20:00:00");
        assertThat(transport.lastRequest.url().getPath()).isEqualTo("/remoteApi/time");
        assertThat(transport.lastRequest.headers()).containsEntry("X-Access-Token", "token-value");
        assertThat(new String(transport.lastRequest.body(), StandardCharsets.UTF_8)).isEqualTo("{}");

        client.close();
        client.close();
        assertThat(transport.closeCalls).hasValue(0);
    }

    @Test
    void missingDefaultProviderFailsDuringBuild() {
        assertThatThrownBy(() -> StationOpenApiClients.builder()
                .endpoint("http://127.0.0.1:23288")
                .accessToken("token-value")
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("No HttpTransportProvider found")
                .hasMessageContaining("inject HttpTransport explicitly");
    }

    @Test
    void explicitTransportSkipsBrokenProviderDiscovery(@TempDir Path serviceRoot) throws Exception {
        writeServiceFile(serviceRoot, "missing.provider.ClassName");
        RecordingTransport transport = new RecordingTransport();

        withContextClassLoader(serviceRoot, () -> {
            StationOpenApiClient client = StationOpenApiClients.builder()
                    .endpoint("http://127.0.0.1:23288")
                    .accessToken("token-value")
                    .transport(transport)
                    .build();
            assertThat(client.system().getTimeText()).isEqualTo("2026-07-10T20:00:00");
            client.close();
        });

        assertThat(transport.closeCalls).hasValue(0);
    }

    @Test
    void discoveredTransportIsSdkOwnedAndReceivesConnectTimeout(@TempDir Path serviceRoot) throws Exception {
        DiscoveredProvider.reset();
        writeServiceFile(serviceRoot, DiscoveredProvider.class.getName());

        withContextClassLoader(serviceRoot, () -> {
            StationOpenApiClient client = StationOpenApiClients.builder()
                    .endpoint("http://127.0.0.1:23288")
                    .accessToken("token-value")
                    .connectTimeout(Duration.ofSeconds(7))
                    .build();
            client.close();
            client.close();
        });

        assertThat(DiscoveredProvider.createCalls).hasValue(1);
        assertThat(DiscoveredProvider.settings.connectTimeout()).isEqualTo(Duration.ofSeconds(7));
        assertThat(DiscoveredProvider.transport.closeCalls).hasValue(1);
    }

    @Test
    void providerCreationFailureIsRedacted(@TempDir Path serviceRoot) throws Exception {
        writeServiceFile(serviceRoot, FailingProvider.class.getName());

        withContextClassLoader(serviceRoot, () -> assertThatThrownBy(() -> StationOpenApiClients.builder()
                .endpoint("http://127.0.0.1:23288")
                .accessToken("raw-token-value")
                .build())
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining(FailingProvider.class.getName())
                .hasMessageNotContaining("provider-secret-value")
                .rootCause()
                .hasMessageNotContaining("provider-secret-value"));
    }

    @Test
    void configToStringRedactsCredentials() {
        StationOpenApiConfig signatureConfig = signatureConfigBuilder().build();
        StationOpenApiConfig tokenConfig = tokenConfigBuilder().build();

        assertThat(signatureConfig.toString())
                .contains("secretKey=<redacted>")
                .doesNotContain("raw-secret-value");
        assertThat(tokenConfig.toString())
                .contains("accessToken=<redacted>")
                .doesNotContain("raw-token-value");
    }

    @Test
    void headerCrLfIsRejectedBeforeTransportWithoutEchoingInput() {
        String injectedValue = "safe-prefix\r\nX-Injected: leaked";
        RecordingTransport transport = new RecordingTransport();

        try (StationOpenApiClient tokenClient = StationOpenApiClients.builder()
                .endpoint("https://station.example.com")
                .accessToken(injectedValue)
                .transport(transport)
                .build()) {
            assertThatThrownBy(() -> tokenClient.system().getTimeText())
                    .isExactlyInstanceOf(StationOpenApiClientException.class)
                    .hasMessageContaining("CR or LF")
                    .hasMessageNotContaining("safe-prefix")
                    .hasMessageNotContaining("leaked");
        }

        try (StationOpenApiClient requestIdClient = StationOpenApiClients.builder()
                .endpoint("https://station.example.com")
                .accessToken("token-value")
                .transport(transport)
                .build()) {
            assertThatThrownBy(() -> requestIdClient.system().getTimeText(
                    RequestOptions.builder().requestId(injectedValue).build()))
                    .isExactlyInstanceOf(StationOpenApiClientException.class)
                    .hasMessageContaining("CR or LF")
                    .hasMessageNotContaining("safe-prefix")
                    .hasMessageNotContaining("leaked");
        }

        assertThatThrownBy(() -> new HttpRequest(
                URI.create("https://station.example.com/remoteApi/time"),
                Map.of("X-Test\r\nX-Injected", "value"), new byte[0], null, null))
                .isExactlyInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("header name");
        assertThatThrownBy(() -> new HttpRequest(
                URI.create("https://station.example.com/remoteApi/time"),
                Map.of("X-Test", injectedValue), new byte[0], null, null))
                .isExactlyInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("header value")
                .hasMessageNotContaining("safe-prefix")
                .hasMessageNotContaining("leaked");

        assertThat(transport.lastRequest).isNull();
    }

    private static StationOpenApiConfig.Builder signatureConfigBuilder() {
        return StationOpenApiConfig.builder()
                .endpoint("http://127.0.0.1:23288")
                .appKey("app")
                .secretKey("raw-secret-value");
    }

    private static StationOpenApiConfig.Builder tokenConfigBuilder() {
        return StationOpenApiConfig.builder()
                .endpoint("https://station.example.com/web")
                .accessToken("raw-token-value");
    }

    private static void assertEndpointRejected(String endpoint, String expectedMessage) {
        assertThatThrownBy(() -> StationOpenApiConfig.builder()
                .endpoint(endpoint)
                .accessToken("token-value")
                .build())
                .isExactlyInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining(expectedMessage)
                .hasMessageNotContaining("uri-password")
                .hasNoCause();
    }

    private static void writeServiceFile(Path serviceRoot, String... providerNames) throws Exception {
        Path serviceFile = serviceRoot.resolve("META-INF/services/")
                .resolve(HttpTransportProvider.class.getName());
        Files.createDirectories(serviceFile.getParent());
        Files.writeString(serviceFile, String.join(System.lineSeparator(), providerNames), StandardCharsets.UTF_8);
    }

    private static Object executorOf(Object api) throws Exception {
        Field field = api.getClass().getDeclaredField("executor");
        field.setAccessible(true);
        return field.get(api);
    }

    private static void withContextClassLoader(Path serviceRoot, ThrowingRunnable action) throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        URL[] urls = {serviceRoot.toUri().toURL()};
        try (URLClassLoader loader = new URLClassLoader(urls, StationOpenApiClientBuilderTest.class.getClassLoader())) {
            thread.setContextClassLoader(loader);
            action.run();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static final class DiscoveredProvider implements HttpTransportProvider {
        private static final AtomicInteger createCalls = new AtomicInteger();
        private static HttpTransportSettings settings;
        private static RecordingTransport transport;

        @Override
        public HttpTransport create(HttpTransportSettings settings) {
            DiscoveredProvider.settings = settings;
            transport = new RecordingTransport();
            createCalls.incrementAndGet();
            return transport;
        }

        private static void reset() {
            createCalls.set(0);
            settings = null;
            transport = null;
        }
    }

    public static final class FailingProvider implements HttpTransportProvider {
        @Override
        public HttpTransport create(HttpTransportSettings settings) {
            throw new IllegalStateException("accessToken=provider-secret-value");
        }
    }

    private static final class RecordingTransport implements HttpTransport {
        private final AtomicInteger closeCalls = new AtomicInteger();
        private HttpRequest lastRequest;

        @Override
        public HttpResponse postJson(HttpRequest request) {
            lastRequest = request;
            String body = "{\"success\":true,\"code\":200,\"msg\":\"OK\","
                    + "\"data\":\"2026-07-10T20:00:00\"}";
            return new HttpResponse(200, Map.of(), body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public HttpResponse download(HttpRequest request, OutputStream outputStream) {
            throw new AssertionError("download is not expected");
        }

        @Override
        public void close() {
            closeCalls.incrementAndGet();
        }
    }
}
