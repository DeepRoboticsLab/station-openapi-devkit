package com.deeprobotics.station.openapi.sdk.transport.okhttp;

import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportProvider;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportSettings;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.ServiceLoader;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

/**
 * OkHttp transport 的 SPI 元数据和 provider 创建语义测试。
 *
 * <p>验证 ServiceLoader 只发现一个默认 provider，且每次创建独立、由 SDK 持有生命周期的 transport，
 * 同时把 core 提供的建连超时传递到实际 OkHttpClient。</p>
 */
class OkHttpTransportProviderTest {

    @Test
    void serviceLoaderFindsExactlyTheOkHttpProvider() {
        List<HttpTransportProvider> providers = ServiceLoader.load(HttpTransportProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();

        assertThat(providers)
                .hasSize(1)
                .first()
                .isInstanceOf(OkHttpTransportProvider.class);
    }

    @Test
    void providerCreatesIndependentSdkOwnedTransportsWithConfiguredTimeout() throws Exception {
        OkHttpTransportProvider provider = new OkHttpTransportProvider();
        OkHttpTransport first = (OkHttpTransport) provider.create(
                new HttpTransportSettings(Duration.ofMillis(1357)));
        OkHttpTransport second = (OkHttpTransport) provider.create(
                new HttpTransportSettings(Duration.ofMillis(2468)));
        OkHttpClient firstClient = clientOf(first);
        OkHttpClient secondClient = clientOf(second);

        try {
            assertThat(first).isNotSameAs(second);
            assertThat(firstClient).isNotSameAs(secondClient);
            assertThat(firstClient.connectTimeoutMillis()).isEqualTo(1357);
            assertThat(secondClient.connectTimeoutMillis()).isEqualTo(2468);

            first.close();
            assertThat(firstClient.dispatcher().executorService().isShutdown()).isTrue();
            assertThat(secondClient.dispatcher().executorService().isShutdown()).isFalse();
        } finally {
            first.close();
            second.close();
        }
    }

    private static OkHttpClient clientOf(HttpTransport transport) throws Exception {
        Field field = OkHttpTransport.class.getDeclaredField("client");
        field.setAccessible(true);
        return (OkHttpClient) field.get(transport);
    }
}
