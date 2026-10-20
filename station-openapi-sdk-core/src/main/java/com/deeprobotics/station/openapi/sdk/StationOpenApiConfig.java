package com.deeprobotics.station.openapi.sdk;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.net.URI;
import java.time.Duration;

/**
 * SDK 全局配置快照。
 *
 * <p>构造时完成归一化和校验，之后保持不可变。</p>
 *
 * <p>签名和 Token 鉴权必须二选一；{@code secretKey/accessToken} 仅保存在内存中，输出时须脱敏。</p>
 */
public final class StationOpenApiConfig {

    /**
     * 客户端配置的鉴权模式，与 {@code auth} 包中的枚举对应。
     */
    public enum AuthMode {
        /**
         * 使用 {@code appKey/secretKey} 生成签名字段的鉴权模式。
         */
        SIGNATURE(com.deeprobotics.station.openapi.sdk.auth.AuthMode.SIGNATURE),
        /**
         * 使用 {@code X-Access-Token} 请求头的鉴权模式。
         */
        ACCESS_TOKEN(com.deeprobotics.station.openapi.sdk.auth.AuthMode.ACCESS_TOKEN);

        private final com.deeprobotics.station.openapi.sdk.auth.AuthMode authMode;

        AuthMode(com.deeprobotics.station.openapi.sdk.auth.AuthMode authMode) {
            this.authMode = authMode;
        }

        /**
         * 转换为 {@code auth} 包使用的鉴权模式。
         *
         * @return 与当前配置值对应的鉴权模式
         */
        public com.deeprobotics.station.openapi.sdk.auth.AuthMode toAuthMode() {
            return authMode;
        }
    }

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(15);

    private final String endpoint;
    private final AuthMode authMode;
    private final String appKey;
    private final String secretKey;
    private final String accessToken;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final Duration callTimeout;

    private StationOpenApiConfig(Builder builder) {
        this.endpoint = normalizeEndpoint(builder.endpoint);
        this.authMode = resolveAuthMode(builder.appKey, builder.secretKey, builder.accessToken);
        this.appKey = blankToNull(builder.appKey);
        this.secretKey = blankToNull(builder.secretKey);
        this.accessToken = blankToNull(builder.accessToken);
        this.connectTimeout = requirePositive(builder.connectTimeout, "connectTimeout");
        this.readTimeout = requirePositive(builder.readTimeout, "readTimeout");
        this.callTimeout = requirePositive(builder.callTimeout, "callTimeout");
    }

    /**
     * 创建配置构造器。默认连接超时为 3 秒、读取超时为 10 秒、调用总超时为 15 秒。
     *
     * @return 带有 SDK 默认超时的配置构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回已经去除尾部斜杠的 OpenAPI endpoint。
     *
     * @return 规范化后的 OpenAPI 服务根地址
     */
    public String endpoint() {
        return endpoint;
    }

    /**
     * 返回校验后唯一生效的鉴权模式。
     *
     * @return 唯一生效的鉴权模式
     */
    public AuthMode authMode() {
        return authMode;
    }

    /**
     * 返回签名模式 appKey；Token 模式下为 {@code null}。
     *
     * @return 签名模式 appKey，或 Token 模式下的 {@code null}
     */
    public String appKey() {
        return appKey;
    }

    /**
     * 返回签名模式 secretKey。
     *
     * <p>返回值为敏感信息，禁止记录或明文持久化；配置摘要使用已脱敏的 {@link #toString()}。</p>
     *
     * @return 签名模式 secretKey，或 Token 模式下的 {@code null}
     */
    public String secretKey() {
        return secretKey;
    }

    /**
     * 返回 Token 模式 accessToken。
     *
     * <p>返回值为敏感信息，禁止记录或明文持久化；配置摘要使用已脱敏的 {@link #toString()}。</p>
     *
     * @return Token 模式 accessToken，或签名模式下的 {@code null}
     */
    public String accessToken() {
        return accessToken;
    }

    /**
     * 返回默认建连超时。
     *
     * @return 大于 0 的默认建连超时
     */
    public Duration connectTimeout() {
        return connectTimeout;
    }

    /**
     * 返回单次阻塞读取的默认等待时限，适用于普通请求和下载，不限制整份文件的传输时长。
     *
     * @return 大于 0 的默认读取超时
     */
    public Duration readTimeout() {
        return readTimeout;
    }

    /**
     * 返回普通 JSON 调用的默认总超时。下载默认不设总时限，可通过 {@link RequestOptions} 单独设置。
     *
     * @return 大于 0 的默认调用总超时
     */
    public Duration callTimeout() {
        return callTimeout;
    }

    @Override
    public String toString() {
        // appKey 用于识别应用；密钥和令牌脱敏。
        return "StationOpenApiConfig{"
                + "endpoint='" + endpoint + '\''
                + ", authMode=" + authMode
                + ", appKey='" + appKey + '\''
                + ", secretKey=" + redact(secretKey)
                + ", accessToken=" + redact(accessToken)
                + ", connectTimeout=" + connectTimeout
                + ", readTimeout=" + readTimeout
                + ", callTimeout=" + callTimeout
                + '}';
    }

    private static String normalizeEndpoint(String endpoint) {
        String trimmed = blankToNull(endpoint);
        if (trimmed == null) {
            throw new StationOpenApiClientException("endpoint is required");
        }

        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            // 不保留 URI 解析异常，避免 user-info 或非法 endpoint 原文进入 cause。
            throw new StationOpenApiClientException("endpoint is invalid");
        }

        String scheme = uri.getScheme();
        if (scheme == null || uri.getHost() == null) {
            throw new StationOpenApiClientException("endpoint must include scheme and host");
        }
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new StationOpenApiClientException("endpoint scheme must be http or https");
        }
        if (uri.getRawUserInfo() != null) {
            throw new StationOpenApiClientException("endpoint user-info is not supported");
        }
        if (uri.getRawQuery() != null) {
            throw new StationOpenApiClientException("endpoint query is not supported");
        }
        if (uri.getRawFragment() != null) {
            throw new StationOpenApiClientException("endpoint fragment is not supported");
        }
        // 保留部署上下文路径，接口路径由 EndpointResolver 拼接。
        String normalized = trimmed;
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static AuthMode resolveAuthMode(String appKey, String secretKey, String accessToken) {
        boolean hasAppKey = hasText(appKey);
        boolean hasSecretKey = hasText(secretKey);
        boolean hasAccessToken = hasText(accessToken);
        boolean hasSignaturePair = hasAppKey && hasSecretKey;

        if (hasAccessToken && (hasAppKey || hasSecretKey)) {
            throw new StationOpenApiClientException("configure exactly one auth mode");
        }
        if (hasAccessToken) {
            return AuthMode.ACCESS_TOKEN;
        }
        if (hasSignaturePair) {
            return AuthMode.SIGNATURE;
        }
        if (hasAppKey) {
            throw new StationOpenApiClientException("secretKey is required when appKey is configured");
        }
        if (hasSecretKey) {
            throw new StationOpenApiClientException("appKey is required when secretKey is configured");
        }
        throw new StationOpenApiClientException("auth configuration is required");
    }

    private static Duration requirePositive(Duration value, String fieldName) {
        if (value == null) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        if (value.isZero() || value.isNegative()) {
            throw new StationOpenApiClientException(fieldName + " must be positive");
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static String redact(String value) {
        return value == null ? "null" : "<redacted>";
    }

    /**
     * 不可变客户端配置的构造器。
     */
    public static final class Builder {
        private String endpoint;
        private String appKey;
        private String secretKey;
        private String accessToken;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;
        private Duration callTimeout = DEFAULT_CALL_TIMEOUT;

        private Builder() {
        }

        /**
         * OpenAPI endpoint，要求为包含 host 的 HTTP(S) 绝对地址，可包含部署 context path。
         * 不能包含 user-info、query 或 fragment。
         *
         * @param endpoint 平台 OpenAPI 服务根地址
         * @return 当前构造器
         */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * 签名模式 appKey，需与 secretKey 成对配置。
         *
         * @param appKey 平台分配的应用标识
         * @return 当前构造器
         */
        public Builder appKey(String appKey) {
            this.appKey = appKey;
            return this;
        }

        /**
         * 签名模式 secretKey，只参与本地签名，不会进入请求体。
         *
         * @param secretKey 平台分配的签名密钥
         * @return 当前构造器
         */
        public Builder secretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        /**
         * Token 模式 accessToken，会放入 {@code X-Access-Token} header。
         *
         * @param accessToken 平台分配的访问令牌
         * @return 当前构造器
         */
        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        /**
         * 默认连接超时。
         *
         * @param connectTimeout 大于 0 的连接超时
         * @return 当前构造器
         */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * 单次阻塞读取的默认等待时限；单次请求可用 {@link RequestOptions} 覆盖。
         *
         * @param readTimeout 大于 0 的默认读取超时
         * @return 当前构造器
         */
        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        /**
         * 普通 JSON 调用的默认总超时，不用于下载；单次请求可用 {@link RequestOptions} 覆盖。
         *
         * @param callTimeout 大于 0 的默认调用总超时
         * @return 当前构造器
         */
        public Builder callTimeout(Duration callTimeout) {
            this.callTimeout = callTimeout;
            return this;
        }

        /**
         * 构建不可变配置快照，并立即完成 endpoint、鉴权模式和超时校验。
         *
         * @return 已校验且不可变的 SDK 全局配置
         */
        public StationOpenApiConfig build() {
            return new StationOpenApiConfig(this);
        }
    }
}
