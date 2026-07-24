package com.deeprobotics.station.openapi.sdk.model.common;

import com.deeprobotics.station.openapi.sdk.exception.Redactor;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiBusinessException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * 平台统一响应 {@code R<T>} 的 SDK 表示。
 *
 * <p>平台业务成功需要同时满足 {@code success=true} 和 {@code code=200}。
 * 历史接口中可能出现 success 为 true 但 code 非 200 的场景，SDK 必须按业务失败处理。</p>
 *
 * <p>{@code toString()} 不直接输出 data 内容。data 可能包含摄像机密码、文件路径或未来敏感字段，
 * 调试时只暴露是否存在，具体内容由调用方显式读取。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ApiResponse<T> {

    /**
     * 平台响应的 success 字段。必须为 true 才代表业务成功。
     */
    private final Boolean success;
    /**
     * 平台响应的 code 字段。SDK 约定只有 200 才代表业务成功。
     */
    private final Integer code;
    /**
     * 平台响应的 msg 字段。SDK 会把它带入业务异常；是否直接展示应由应用的国际化和错误提示策略决定。
     */
    private final String msg;
    /**
     * 平台响应的 data 字段。具体类型由调用方传入的 {@code TypeRef<ApiResponse<T>>} 决定。
     */
    private final T data;

    /**
     * 创建平台 {@code R<T>} 响应快照，主要由 Jackson 反序列化调用。
     *
     * @param success 平台业务成功标记
     * @param code 平台业务状态码
     * @param msg 平台业务消息
     * @param data 业务数据
     */
    @JsonCreator
    public ApiResponse(@JsonProperty("success") Boolean success, @JsonProperty("code") Integer code,
            @JsonProperty("msg") String msg, @JsonProperty("data") T data) {
        this.success = success;
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 创建测试或内部使用的标准成功响应。
     *
     * @param <T> 业务数据类型
     * @param data 成功响应携带的业务数据
     * @return {@code success=true/code=200} 的响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "OK", data);
    }

    /**
     * 返回平台 success 标记。
     *
     * @return 平台 success 原值，字段缺失时为 {@code null}
     */
    public Boolean success() {
        return success;
    }

    /**
     * 返回平台业务 code。
     *
     * @return 平台业务 code，字段缺失时为 {@code null}
     */
    public Integer code() {
        return code;
    }

    /**
     * 返回平台业务消息。
     *
     * @return 平台 msg 原值
     */
    public String msg() {
        return msg;
    }

    /**
     * 返回业务数据。
     *
     * @return 已按目标泛型解析的 data，响应无数据时为 {@code null}
     */
    public T data() {
        return data;
    }

    /**
     * 判断响应是否同时满足 {@code success=true} 和 {@code code=200}。
     *
     * @return 同时满足两个条件时返回 {@code true}
     */
    public boolean isSuccessful() {
        // 兼容 success 和 code 不完全一致的响应；SDK 必须同时检查两个字段。
        return Boolean.TRUE.equals(success) && Integer.valueOf(200).equals(code);
    }

    /**
     * 按 SDK 统一规则解包业务数据。
     *
     * <p>这里不处理 HTTP status；HTTP 非 2xx 在 executor 中先转换为
     * {@code StationOpenApiHttpException}。进入这里说明响应体已按 {@code R<T>} 解析。</p>
     *
     * @param path 本次请求的 {@code /remoteApi/*} 路径
     * @param requestId 本次请求链路标识
     * @param operation SDK 业务操作标识
     * @return 业务成功时的 data
     */
    public T unwrapOrThrow(String path, String requestId, String operation) {
        if (!isSuccessful()) {
            throw new StationOpenApiBusinessException(code, msg, path, requestId, operation);
        }
        return data;
    }

    @Override
    public String toString() {
        return "ApiResponse{"
                + "success=" + success
                + ", code=" + code
                + ", msg='" + Redactor.redact(msg) + '\''
                // 避免日志中展开完整 data；客户要看业务数据应在自己的代码中显式输出并自行脱敏。
                + ", data=" + (data == null ? "null" : "<present>")
                + '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ApiResponse<?> that)) {
            return false;
        }
        return Objects.equals(success, that.success)
                && Objects.equals(code, that.code)
                && Objects.equals(msg, that.msg)
                && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, code, msg, data);
    }
}
