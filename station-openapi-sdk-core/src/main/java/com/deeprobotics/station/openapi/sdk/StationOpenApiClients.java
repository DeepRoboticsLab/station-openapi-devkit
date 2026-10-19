package com.deeprobotics.station.openapi.sdk;

import com.deeprobotics.station.openapi.sdk.auth.AccessTokenAuthProvider;
import com.deeprobotics.station.openapi.sdk.auth.AuthProvider;
import com.deeprobotics.station.openapi.sdk.auth.Credential;
import com.deeprobotics.station.openapi.sdk.auth.SignatureAuthProvider;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportProvider;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportSettings;
import com.deeprobotics.station.openapi.sdk.internal.DefaultStationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.internal.HttpTransportProviderLoader;
import com.deeprobotics.station.openapi.sdk.internal.OpenApiExecutor;
import java.time.Duration;
import java.util.ServiceConfigurationError;

/**
 * 创建 {@link StationOpenApiClient} 的统一工厂。
 *
 * <p>普通客户只需引入 {@code station-openapi-sdk}，默认 OkHttp transport 会通过 JDK SPI 自动发现。
 * core 只依赖 {@link HttpTransport} 抽象，不依赖 OkHttp；高级客户也可以显式注入其他 transport。</p>
 */
public final class StationOpenApiClients {

    private StationOpenApiClients() {
    }

    /**
     * 创建客户端构造器。
     *
     * @return 新的客户端构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 客户端构造器，支持默认或自定义 HTTP 传输实现。
     */
    public static final class Builder {
        private final StationOpenApiConfig.Builder configBuilder = StationOpenApiConfig.builder();
        private HttpTransport transport;

        private Builder() {
        }

        /**
         * 配置平台 HTTP(S) OpenAPI endpoint，可包含 context path，例如 {@code https://host/web}，
         * 但不能包含 user-info、query 或 fragment。
         *
         * @param endpoint 包含 scheme 和 host 的平台地址
         * @return 当前构造器
         */
        public Builder endpoint(String endpoint) {
            configBuilder.endpoint(endpoint);
            return this;
        }

        /**
         * 签名模式 appKey。必须与 {@link #secretKey(String)} 同时配置，且不能再配置 accessToken。
         *
         * @param appKey 平台分配的应用标识
         * @return 当前构造器
         */
        public Builder appKey(String appKey) {
            configBuilder.appKey(appKey);
            return this;
        }

        /**
         * 签名模式 secretKey。只用于本地签名，绝不能发送到平台或进入日志、异常和字符串输出。
         *
         * @param secretKey 平台分配的签名密钥
         * @return 当前构造器
         */
        public Builder secretKey(String secretKey) {
            configBuilder.secretKey(secretKey);
            return this;
        }

        /**
         * Token 模式凭证。请求通过 {@code X-Access-Token} header 鉴权，不发送签名字段。
         *
         * @param accessToken 平台分配的访问令牌
         * @return 当前构造器
         */
        public Builder accessToken(String accessToken) {
            configBuilder.accessToken(accessToken);
            return this;
        }

        /**
         * 连接超时，默认 3 秒。该配置会传给自动发现的默认 transport。
         *
         * @param connectTimeout 大于 0 的连接超时
         * @return 当前构造器
         */
        public Builder connectTimeout(Duration connectTimeout) {
            configBuilder.connectTimeout(connectTimeout);
            return this;
        }

        /**
         * 读取超时，默认 10 秒；单次调用可通过 {@link RequestOptions} 覆盖。
         *
         * @param readTimeout 大于 0 的默认读取超时
         * @return 当前构造器
         */
        public Builder readTimeout(Duration readTimeout) {
            configBuilder.readTimeout(readTimeout);
            return this;
        }

        /**
         * 单次调用总超时，默认 15 秒；单次调用可通过 {@link RequestOptions} 覆盖。
         *
         * @param callTimeout 大于 0 的默认调用总超时
         * @return 当前构造器
         */
        public Builder callTimeout(Duration callTimeout) {
            configBuilder.callTimeout(callTimeout);
            return this;
        }

        /**
         * 显式注入 HTTP transport，并跳过默认 SPI 自动发现。
         *
         * <p>注入对象由调用方管理，关闭客户端不会关闭该 transport。</p>
         *
         * @param transport 客户创建并负责关闭的 HTTP transport
         * @return 当前构造器
         */
        public Builder transport(HttpTransport transport) {
            if (transport == null) {
                throw new StationOpenApiClientException("transport is required");
            }
            this.transport = transport;
            return this;
        }

        /**
         * 构建客户端，装配鉴权、传输实现及七个业务 API 分组。
         *
         * <p>未显式注入 transport 时，classpath 中必须恰好存在一个
         * {@link HttpTransportProvider}。缺失、冲突或损坏的 SPI 注册会在此处直接失败。</p>
         *
         * @return 已装配的客户端
         */
        public StationOpenApiClient build() {
            StationOpenApiConfig config = configBuilder.build();
            // 鉴权先于 transport 分配，避免凭证配置失败后留下网络资源。
            AuthProvider authProvider = createAuthProvider(config);

            if (transport != null) {
                try {
                    return assemble(config, authProvider, transport, false);
                } catch (RuntimeException | ServiceConfigurationError | LinkageError failure) {
                    throw new StationOpenApiClientException("Failed to assemble StationOpenApiClient", failure);
                }
            }

            HttpTransportProvider provider = HttpTransportProviderLoader.load();
            HttpTransport discoveredTransport = createTransport(provider,
                    new HttpTransportSettings(config.connectTimeout()));
            try {
                // SPI 创建的 transport 由 SDK 负责关闭，装配失败时也须释放。
                return assemble(config, authProvider, discoveredTransport, true);
            } catch (RuntimeException | ServiceConfigurationError | LinkageError failure) {
                throw assemblyFailure(discoveredTransport, failure);
            }
        }

        private static AuthProvider createAuthProvider(StationOpenApiConfig config) {
            return switch (config.authMode()) {
                case SIGNATURE -> new SignatureAuthProvider(
                        Credential.signature(config.appKey(), config.secretKey()));
                case ACCESS_TOKEN -> new AccessTokenAuthProvider(
                        Credential.accessToken(config.accessToken()));
            };
        }

        private static HttpTransport createTransport(HttpTransportProvider provider,
                HttpTransportSettings settings) {
            String providerClass = provider.getClass().getName();
            try {
                HttpTransport created = provider.create(settings);
                if (created == null) {
                    throw new StationOpenApiClientException(
                            "HttpTransportProvider returned null: " + providerClass);
                }
                return created;
            } catch (RuntimeException | ServiceConfigurationError | LinkageError failure) {
                throw new StationOpenApiClientException(
                        "Failed to create HttpTransport with provider " + providerClass, failure);
            }
        }

        private static StationOpenApiClient assemble(StationOpenApiConfig config, AuthProvider authProvider,
                HttpTransport transport, boolean transportOwnedBySdk) {
            OpenApiExecutor executor = new OpenApiExecutor(config, authProvider, transport);
            return new DefaultStationOpenApiClient(executor, transport, transportOwnedBySdk);
        }

        private static StationOpenApiClientException assemblyFailure(HttpTransport transport, Throwable failure) {
            StationOpenApiClientException wrapped = new StationOpenApiClientException(
                    "Failed to assemble StationOpenApiClient", failure);
            try {
                transport.close();
            } catch (RuntimeException | ServiceConfigurationError | LinkageError closeFailure) {
                wrapped.addSuppressed(new StationOpenApiClientException(
                        "Failed to close SDK-owned HttpTransport after client assembly failure", closeFailure));
            }
            return wrapped;
        }
    }
}
