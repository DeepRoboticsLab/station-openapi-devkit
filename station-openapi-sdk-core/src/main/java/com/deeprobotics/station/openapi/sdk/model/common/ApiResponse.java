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
 * <p>业务成功须同时满足 {@code success=true} 和 {@code code=200}，两者不一致时按失败处理。</p>
 *
 * <p>{@code toString()} 仅标记 data 是否存在，避免输出摄像机密码等敏感内容。</p>
 *
 * @param <T> 业务数据类型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ApiResponse<T> {

    private final Boolean success;
    private final Integer code;
    private final String msg;
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
     * 创建标准成功响应。
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
     * 返回平台业务消息。失败时会带入业务异常，展示文案由应用决定。
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
        return Boolean.TRUE.equals(success) && Integer.valueOf(200).equals(code);
    }

    /**
     * 解包业务数据，业务失败时抛出 {@link StationOpenApiBusinessException}。HTTP 状态由执行器先行校验。
     *
     * @param path 本次请求的 {@code /remoteApi/*} 路径
     * @param requestId 本次请求链路标识
     * @param operation SDK 业务操作标识
     * @return 业务成功时的 data，可为 {@code null}
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
