package com.deeprobotics.station.openapi.sdk;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.net.URI;
import java.time.Duration;

/**
 * SDK 全局配置快照。
 *
 * <p>配置对象在构造时完成归一化和校验，之后保持不可变。这样后续 executor、auth provider
 * 和 transport 只读取已经确认过的值，不需要在每次请求时重复处理客户输入。</p>
 *
 * <p>安全约束：签名模式和 Token 模式必须二选一；{@code secretKey/accessToken}
 * 只允许保存在内存配置中，任何字符串化输出都必须脱敏。</p>
 */
public final class StationOpenApiConfig {

    /**
     * 客户配置使用的鉴权模式枚举。
     *
     * <p>该枚举与 {@code auth} 包中的公共鉴权模式对应，并为配置构造器提供稳定入口。</p>
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

        /**
         * {@code auth} 包中与当前配置值对应的鉴权模式。
         */
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
    /**
     * 默认读取超时。查询接口和普通 JSON 接口以较短读超时快速失败；下载场景可用 {@link RequestOptions} 覆盖。
     */
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);
    /**
     * 默认调用总超时，约束一次请求从开始到结束的最长时间。
     */
    private static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(15);

    /**
     * OpenAPI 服务根地址，构造时已经去除尾部斜杠。
     *
     * <p>允许包含部署 context path，例如 {@code https://station.example.com/web}。</p>
     */
    private final String endpoint;
    /**
     * 已解析完成的鉴权模式。该值保证是签名模式或 Token 模式中的一种，不存在混配状态。
     */
    private final AuthMode authMode;
    /**
     * 签名模式 appKey。Token 模式下为 {@code null}。
     */
    private final String appKey;
    /**
     * 签名模式 secretKey。只保存在内存中供签名使用，不能进入请求体、日志、异常或 {@code toString()}。
     */
    private final String secretKey;
    /**
     * Token 模式 accessToken。只用于构造鉴权 header，字符串化输出必须脱敏。
     */
    private final String accessToken;
    /**
     * 默认连接超时，传给底层 HTTP 实现用于建连阶段。
     */
    private final Duration connectTimeout;
    /**
     * 默认读取超时，单次请求可通过 {@link RequestOptions#readTimeout()} 覆盖。
     */
    private final Duration readTimeout;
    /**
     * 默认调用总超时，单次请求可通过 {@link RequestOptions#callTimeout()} 覆盖。
     */
    private final Duration callTimeout;

    private StationOpenApiConfig(Builder builder) {
        // endpoint 先归一化，避免后续 EndpointResolver 处理重复尾斜杠时出现多套规则。
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
     * 返回已经校验过的鉴权模式。调用方不需要再判断凭证是否混配。
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
     * <p>这是敏感值访问器。不要把返回值写入日志、异常、监控标签或持久化明文；
     * 需要输出配置摘要时使用已脱敏的 {@link #toString()}。</p>
     *
     * @return 签名模式 secretKey，或 Token 模式下的 {@code null}
     */
    public String secretKey() {
        return secretKey;
    }

    /**
     * 返回 Token 模式 accessToken。
     *
     * <p>这是敏感值访问器。不要记录、回显或以明文持久化返回值；
     * 需要输出配置摘要时使用已脱敏的 {@link #toString()}。</p>
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
     * 返回默认读取超时。文件下载不会自动放大该值，长下载应通过 {@link RequestOptions} 显式覆盖。
     *
     * @return 大于 0 的默认读取超时
     */
    public Duration readTimeout() {
        return readTimeout;
    }

    /**
     * 返回默认调用总超时。长下载若只放大读取超时而总超时仍较短，会先被总超时中断；两者应协调配置。
     *
     * @return 大于 0 的默认调用总超时
     */
    public Duration callTimeout() {
        return callTimeout;
    }

    @Override
    public String toString() {
        // appKey 本身不是密钥，保留有助于客户排查配置；secretKey/token 永远脱敏。
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
        // 只去掉尾部斜杠，不拆分 host/port/contextPath；最终 path 拼接由 EndpointResolver 统一完成。
        String normalized = trimmed;
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static AuthMode resolveAuthMode(String appKey, String secretKey, String accessToken) {
        // SDK 禁止混配两种凭证，确保每个客户端只有一种明确的鉴权模式。
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
     * 收集客户输入并构建不可变 {@link StationOpenApiConfig} 的配置构造器。
     */
    public static final class Builder {
        /**
         * 客户输入的 endpoint 原始值，最终由 {@link StationOpenApiConfig} 构造器归一化。
         */
        private String endpoint;
        /**
         * 签名模式 appKey 原始值，构造时会 trim 并和 secretKey 成对校验。
         */
        private String appKey;
        /**
         * 签名模式 secretKey 原始值，构造时只做内存保存和脱敏约束。
         */
        private String secretKey;
        /**
         * Token 模式 accessToken 原始值，构造时会与签名凭证互斥校验。
         */
        private String accessToken;
        /**
         * 默认连接超时，未配置时使用 SDK 设计默认值。
         */
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        /**
         * 默认读取超时，未配置时使用 SDK 设计默认值。
         */
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;
        /**
         * 默认调用总超时，未配置时使用 SDK 设计默认值。
         */
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
         * 默认读取超时；单次请求可用 {@link RequestOptions} 覆盖。
         *
         * @param readTimeout 大于 0 的默认读取超时
         * @return 当前构造器
         */
        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        /**
         * 默认调用总超时；单次请求可用 {@link RequestOptions} 覆盖。
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
