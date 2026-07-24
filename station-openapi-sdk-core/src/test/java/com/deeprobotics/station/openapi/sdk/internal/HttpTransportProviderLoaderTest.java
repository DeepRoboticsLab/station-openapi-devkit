package com.deeprobotics.station.openapi.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportProvider;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportSettings;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 默认 HTTP transport 的 JDK SPI 发现规则测试。
 *
 * <p>使用临时 {@code META-INF/services} 和隔离 ClassLoader 覆盖无 provider、唯一 provider、多个 provider、
 * 损坏注册和构造失败。多个候选时必须在实例化前确定性失败，防止任意 provider 构造器产生副作用。</p>
 */
class HttpTransportProviderLoaderTest {

    @BeforeEach
    void resetConstructorCounts() {
        FirstProvider.constructorCalls.set(0);
        SecondProvider.constructorCalls.set(0);
    }

    @Test
    void settingsRequirePositiveConnectTimeout() {
        assertThat(new HttpTransportSettings(Duration.ofSeconds(3)).connectTimeout())
                .isEqualTo(Duration.ofSeconds(3));
        assertThatThrownBy(() -> new HttpTransportSettings(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connectTimeout");
    }

    @Test
    void noProviderFailsWithActionableMessage(@TempDir Path serviceRoot) throws Exception {
        try (URLClassLoader loader = newLoader(serviceRoot)) {
            assertThatThrownBy(() -> HttpTransportProviderLoader.load(loader))
                    .isInstanceOf(StationOpenApiClientException.class)
                    .hasMessageContaining("No HttpTransportProvider found")
                    .hasMessageContaining("station-openapi-sdk")
                    .hasMessageContaining("inject HttpTransport explicitly");
        }
    }

    @Test
    void nullThreadContextClassLoaderFallsBackToSpiClassLoader() {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(null);
            assertThatThrownBy(HttpTransportProviderLoader::load)
                    .isInstanceOf(StationOpenApiClientException.class)
                    .hasMessageContaining("No HttpTransportProvider found");
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @Test
    void uniqueProviderIsInstantiated(@TempDir Path serviceRoot) throws Exception {
        writeServiceFile(serviceRoot, FirstProvider.class.getName());

        try (URLClassLoader loader = newLoader(serviceRoot)) {
            HttpTransportProvider provider = HttpTransportProviderLoader.load(loader);

            assertThat(provider).isInstanceOf(FirstProvider.class);
            assertThat(FirstProvider.constructorCalls).hasValue(1);
        }
    }

    @Test
    void multipleProvidersFailInSortedOrderWithoutInstantiation(@TempDir Path serviceRoot) throws Exception {
        writeServiceFile(serviceRoot, SecondProvider.class.getName(), FirstProvider.class.getName());

        try (URLClassLoader loader = newLoader(serviceRoot)) {
            assertThatThrownBy(() -> HttpTransportProviderLoader.load(loader))
                    .isInstanceOf(StationOpenApiClientException.class)
                    .hasMessageContaining("Multiple HttpTransportProvider implementations found")
                    .hasMessageContaining("[" + FirstProvider.class.getName() + ", "
                            + SecondProvider.class.getName() + "]");
            assertThat(FirstProvider.constructorCalls).hasValue(0);
            assertThat(SecondProvider.constructorCalls).hasValue(0);
        }
    }

    @Test
    void malformedRegistrationIsConvertedToRedactedSdkException(@TempDir Path serviceRoot) throws Exception {
        writeServiceFile(serviceRoot, "missing.provider.ClassName");

        try (URLClassLoader loader = newLoader(serviceRoot)) {
            assertThatThrownBy(() -> HttpTransportProviderLoader.load(loader))
                    .isInstanceOf(StationOpenApiClientException.class)
                    .hasMessageContaining("Failed to load HttpTransportProvider")
                    .hasMessageNotContaining("accessToken")
                    .hasRootCauseInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void providerConstructorFailureIsRedacted(@TempDir Path serviceRoot) throws Exception {
        writeServiceFile(serviceRoot, FailingConstructorProvider.class.getName());

        try (URLClassLoader loader = newLoader(serviceRoot)) {
            assertThatThrownBy(() -> HttpTransportProviderLoader.load(loader))
                    .isInstanceOf(StationOpenApiClientException.class)
                    .hasMessageNotContaining("constructor-secret-value")
                    .rootCause()
                    .hasMessageNotContaining("constructor-secret-value");
        }
    }

    private static URLClassLoader newLoader(Path serviceRoot) throws Exception {
        URL[] urls = {serviceRoot.toUri().toURL()};
        return new URLClassLoader(urls, HttpTransportProviderLoaderTest.class.getClassLoader());
    }

    private static void writeServiceFile(Path serviceRoot, String... providerNames) throws Exception {
        Path serviceFile = serviceRoot.resolve("META-INF/services/")
                .resolve(HttpTransportProvider.class.getName());
        Files.createDirectories(serviceFile.getParent());
        Files.writeString(serviceFile, String.join(System.lineSeparator(), providerNames), StandardCharsets.UTF_8);
    }

    public static final class FirstProvider implements HttpTransportProvider {
        private static final AtomicInteger constructorCalls = new AtomicInteger();

        public FirstProvider() {
            constructorCalls.incrementAndGet();
        }

        @Override
        public HttpTransport create(HttpTransportSettings settings) {
            return new NoopTransport();
        }
    }

    public static final class SecondProvider implements HttpTransportProvider {
        private static final AtomicInteger constructorCalls = new AtomicInteger();

        public SecondProvider() {
            constructorCalls.incrementAndGet();
        }

        @Override
        public HttpTransport create(HttpTransportSettings settings) {
            return new NoopTransport();
        }
    }

    public static final class FailingConstructorProvider implements HttpTransportProvider {
        public FailingConstructorProvider() {
            throw new IllegalStateException("secretKey=constructor-secret-value");
        }

        @Override
        public HttpTransport create(HttpTransportSettings settings) {
            return new NoopTransport();
        }
    }

    private static final class NoopTransport implements HttpTransport {
        @Override
        public HttpResponse postJson(HttpRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HttpResponse download(HttpRequest request, OutputStream outputStream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }
}
