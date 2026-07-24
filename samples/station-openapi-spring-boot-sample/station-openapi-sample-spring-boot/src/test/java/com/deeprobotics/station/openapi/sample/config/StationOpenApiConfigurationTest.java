package com.deeprobotics.station.openapi.sample.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class StationOpenApiConfigurationTest {

    private static final String ENDPOINT = "http://127.0.0.1:23288";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(StationOpenApiConfiguration.class);

    @Test
    void tokenModeCreatesExactlyOneClient() {
        runner.withPropertyValues(
                "station.openapi.endpoint=" + ENDPOINT,
                "station.openapi.auth-mode=ACCESS_TOKEN",
                "station.openapi.access-token=test-token")
                .run(context -> assertThat(context).hasSingleBean(StationOpenApiClient.class));
    }

    @Test
    void signatureModeCreatesExactlyOneClient() {
        runner.withPropertyValues(
                "station.openapi.endpoint=" + ENDPOINT,
                "station.openapi.auth-mode=SIGNATURE",
                "station.openapi.app-key=test-app",
                "station.openapi.secret-key=test-secret")
                .run(context -> assertThat(context).hasSingleBean(StationOpenApiClient.class));
    }

    @Test
    void missingAuthModeFailsStartup() {
        runner.withPropertyValues("station.openapi.endpoint=" + ENDPOINT)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void tokenModeRejectsSignatureCredentials() {
        runner.withPropertyValues(
                "station.openapi.endpoint=" + ENDPOINT,
                "station.openapi.auth-mode=ACCESS_TOKEN",
                "station.openapi.access-token=test-token",
                "station.openapi.app-key=test-app",
                "station.openapi.secret-key=test-secret")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void signatureModeRejectsAccessToken() {
        runner.withPropertyValues(
                "station.openapi.endpoint=" + ENDPOINT,
                "station.openapi.auth-mode=SIGNATURE",
                "station.openapi.app-key=test-app",
                "station.openapi.secret-key=test-secret",
                "station.openapi.access-token=test-token")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void nonPositiveTimeoutsFailStartup() {
        List.of(
                "station.openapi.connect-timeout=0s",
                "station.openapi.read-timeout=-1s",
                "station.openapi.call-timeout=0s")
                .forEach(invalidTimeout -> runner.withPropertyValues(
                        "station.openapi.endpoint=" + ENDPOINT,
                        "station.openapi.auth-mode=ACCESS_TOKEN",
                        "station.openapi.access-token=test-token",
                        invalidTimeout)
                        .run(context -> assertThat(context).hasFailed()));
    }

    @Test
    void propertiesUseSdkTimeoutDefaultsAndDoNotExposeCredentials() {
        StationOpenApiProperties properties = new StationOpenApiProperties();
        properties.setSecretKey("test-secret");
        properties.setAccessToken("test-token");

        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.getCallTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(properties.toString())
                .doesNotContain("test-secret")
                .doesNotContain("test-token");
    }
}
