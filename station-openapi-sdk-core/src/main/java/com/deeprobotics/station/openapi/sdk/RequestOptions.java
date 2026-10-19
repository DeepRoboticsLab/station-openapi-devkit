package com.deeprobotics.station.openapi.sdk;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.time.Duration;

/**
 * 单次 OpenAPI 调用的运行时选项。
 *
 * <p>覆盖本次调用的超时、请求 ID 和重试开关，不影响客户端全局配置。</p>
 *
 * <p>下载、写入和控制请求均不自动重试，也不能通过此选项开启。</p>
 */
public final class RequestOptions {

    /**
     * 请求级重试开关。
     */
    public enum RetryPolicy {
        /**
         * 使用 {@code OperationIntent} 对应的默认次数：查询最多 2 次，下载/写入/控制 1 次。
         */
        DEFAULT,
        /**
         * 禁止 SDK 自动重试，适合客户自行做幂等控制或链路排障。
         */
        NEVER
    }

    private final String requestId;
    private final Duration readTimeout;
    private final Duration callTimeout;
    private final RetryPolicy retryPolicy;

    private RequestOptions(Builder builder) {
        this.requestId = blankToNull(builder.requestId);
        this.readTimeout = optionalPositive(builder.readTimeout, "readTimeout");
        this.callTimeout = optionalPositive(builder.callTimeout, "callTimeout");
        this.retryPolicy = requireRetryPolicy(builder.retryPolicy);
    }

    /**
     * 创建请求级选项构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回客户请求 ID；未配置时为 {@code null}，实际调用时由 SDK 生成 UUID。仅用于追踪，不作为幂等键。
     *
     * @return 客户请求 ID 或 {@code null}
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 返回读取超时覆盖值；为 {@code null} 时使用 {@link StationOpenApiConfig#readTimeout()}。
     *
     * @return 读取超时覆盖值或 {@code null}
     */
    public Duration readTimeout() {
        return readTimeout;
    }

    /**
     * 返回总超时覆盖值；为 {@code null} 时使用 {@link StationOpenApiConfig#callTimeout()}。
     *
     * @return 调用总超时覆盖值或 {@code null}
     */
    public Duration callTimeout() {
        return callTimeout;
    }

    /**
     * 返回本次调用的重试开关。
     *
     * @return 非 null 的请求级重试开关
     */
    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static Duration optionalPositive(Duration value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isZero() || value.isNegative()) {
            throw new StationOpenApiClientException(fieldName + " must be positive when configured");
        }
        return value;
    }

    private static RetryPolicy requireRetryPolicy(RetryPolicy retryPolicy) {
        if (retryPolicy == null) {
            throw new StationOpenApiClientException("retryPolicy is required");
        }
        return retryPolicy;
    }

    /**
     * 单次调用选项构造器。未设置的超时字段继续使用客户端全局配置。
     */
    public static final class Builder {
        private String requestId;
        private Duration readTimeout;
        private Duration callTimeout;
        private RetryPolicy retryPolicy = RetryPolicy.DEFAULT;

        private Builder() {
        }

        /**
         * 设置追踪 ID，透传至 {@code X-Request-Id} 和异常上下文；不作为幂等键。
         *
         * @param requestId 客户链路标识；null 或空白表示由 SDK 生成
         * @return 当前构造器
         */
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * 覆盖本次调用读取超时。
         *
         * @param readTimeout 正数读取超时，传 {@code null} 时使用客户端默认值
         * @return 当前构造器
         */
        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        /**
         * 覆盖本次调用总超时。
         *
         * @param callTimeout 正数调用总超时，传 {@code null} 时使用客户端默认值
         * @return 当前构造器
         */
        public Builder callTimeout(Duration callTimeout) {
            this.callTimeout = callTimeout;
            return this;
        }

        /**
         * 覆盖本次调用的重试开关。
         *
         * @param retryPolicy 非 null 的请求级重试开关
         * @return 当前构造器
         */
        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        /**
         * 构建不可变请求选项，空白 requestId 会被视为未配置。
         *
         * @return 不可变的请求级选项
         */
        public RequestOptions build() {
            return new RequestOptions(this);
        }
    }
}
