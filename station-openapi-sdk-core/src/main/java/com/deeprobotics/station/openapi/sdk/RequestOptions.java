package com.deeprobotics.station.openapi.sdk;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.time.Duration;

/**
 * 单次 OpenAPI 调用的运行时选项。
 *
 * <p>它用于覆盖全局配置中不应该被永久改变的参数，例如一次下载需要更长 read timeout，
 * 或客户希望把自己的链路 ID 透传到 {@code X-Request-Id}。</p>
 *
 * <p>注意：重试策略只控制“是否按 SDK 默认规则重试”。危险操作的默认规则仍是不重试，
 * 因为任务/设备/摄像机控制在连接中断后可能已经被平台或设备接收。</p>
 */
public final class RequestOptions {

    /**
     * 请求级重试开关。
     */
    public enum RetryPolicy {
        /**
         * 使用 {@code OperationIntent} 对应的默认次数：查询和下载最多 2 次，写/控制 1 次。
         */
        DEFAULT,
        /**
         * 禁止 SDK 自动重试，适合客户自行做幂等控制或链路排障。
         */
        NEVER
    }

    /**
     * 客户侧链路 ID。为 {@code null} 时由 executor 自动生成 UUID。
     *
     * <p>该值会进入 {@code X-Request-Id}，并随异常上下文返回，便于串联调用方、SDK 与平台日志。
     * 它仅用于链路追踪，不是业务幂等键；重复使用同一个值不会让写入或控制请求变成可安全重试。</p>
     */
    private final String requestId;
    /**
     * 本次调用读取超时覆盖值。为 {@code null} 时使用 {@link StationOpenApiConfig#readTimeout()}。
     */
    private final Duration readTimeout;
    /**
     * 本次调用总超时覆盖值。为 {@code null} 时使用 {@link StationOpenApiConfig#callTimeout()}。
     */
    private final Duration callTimeout;
    /**
     * 本次调用重试开关。它只在 SDK 安全默认规则内生效，不会把危险操作自动放大为多次尝试。
     */
    private final RetryPolicy retryPolicy;

    private RequestOptions(Builder builder) {
        // 空白 requestId 不向下透传，executor 会生成 UUID，保证每次请求都有可排查的链路标识。
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
     * 返回客户指定的请求 ID；未配置时为 {@code null}，executor 会为实际调用生成 UUID。
     * 该值仅用于链路追踪，不能用作 SDK 或平台的业务去重依据。
     *
     * @return 客户请求 ID 或 {@code null}
     */
    public String requestId() {
        return requestId;
    }

    /**
     * 返回本次调用的读取超时覆盖值；未配置时为 {@code null}。
     *
     * @return 读取超时覆盖值或 {@code null}
     */
    public Duration readTimeout() {
        return readTimeout;
    }

    /**
     * 返回本次调用的总超时覆盖值；未配置时为 {@code null}。
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
         * 客户自定义请求 ID。会进入 {@code X-Request-Id} header 和 SDK 异常上下文。
         * 该值不是业务幂等键，重复使用不会使写入或控制请求可安全重试。
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
