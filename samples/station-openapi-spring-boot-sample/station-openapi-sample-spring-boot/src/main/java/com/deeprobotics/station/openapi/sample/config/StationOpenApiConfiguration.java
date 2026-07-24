package com.deeprobotics.station.openapi.sample.config;

import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClients;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 根据配置创建全应用共享的 {@link StationOpenApiClient}。
 *
 * <p>所有必填项在启动阶段 fail-fast，并且签名与 Token 凭证必须互斥，避免应用带着模糊鉴权配置运行。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StationOpenApiProperties.class)
public class StationOpenApiConfiguration {

    /**
     * 创建单例 SDK Client，并由 Spring 在应用关闭时调用 {@code close()} 释放连接资源。
     */
    @Bean(destroyMethod = "close")
    StationOpenApiClient stationOpenApiClient(StationOpenApiProperties properties) {
        StationOpenApiProperties.AuthMode authMode = requireAuthMode(properties);
        // 先拒绝非当前模式的凭证，避免环境变量残留造成鉴权方式不明确。
        rejectMixedCredentials(properties, authMode);

        StationOpenApiClients.Builder builder = StationOpenApiClients.builder()
                .endpoint(requireText(properties.getEndpoint(), "station.openapi.endpoint"))
                .connectTimeout(requirePositive(
                        properties.getConnectTimeout(), "station.openapi.connect-timeout"))
                .readTimeout(requirePositive(
                        properties.getReadTimeout(), "station.openapi.read-timeout"))
                .callTimeout(requirePositive(
                        properties.getCallTimeout(), "station.openapi.call-timeout"));

        return switch (authMode) {
            case SIGNATURE -> builder
                    .appKey(requireText(properties.getAppKey(), "station.openapi.app-key"))
                    .secretKey(requireText(properties.getSecretKey(), "station.openapi.secret-key"))
                    .build();
            case ACCESS_TOKEN -> builder
                    .accessToken(requireText(properties.getAccessToken(), "station.openapi.access-token"))
                    .build();
        };
    }

    private static StationOpenApiProperties.AuthMode requireAuthMode(StationOpenApiProperties properties) {
        if (properties.getAuthMode() == null) {
            throw new IllegalStateException("station.openapi.auth-mode is required");
        }
        return properties.getAuthMode();
    }

    private static void rejectMixedCredentials(
            StationOpenApiProperties properties, StationOpenApiProperties.AuthMode authMode) {
        if (authMode == StationOpenApiProperties.AuthMode.SIGNATURE
                && hasText(properties.getAccessToken())) {
            throw new IllegalStateException(
                    "station.openapi.access-token must not be configured in signature mode");
        }
        if (authMode == StationOpenApiProperties.AuthMode.ACCESS_TOKEN
                && (hasText(properties.getAppKey()) || hasText(properties.getSecretKey()))) {
            throw new IllegalStateException(
                    "station.openapi.app-key and station.openapi.secret-key must not be configured in token mode");
        }
    }

    private static String requireText(String value, String propertyName) {
        if (!hasText(value)) {
            throw new IllegalStateException(propertyName + " is required");
        }
        return value.trim();
    }

    private static Duration requirePositive(Duration value, String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(propertyName + " must be positive");
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
