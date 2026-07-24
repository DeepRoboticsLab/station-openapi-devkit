package com.deeprobotics.station.openapi.sdk.exception;

/**
 * SDK 本地配置或参数构造异常。
 *
 * <p>这类错误在请求发出前发生，例如 endpoint 无效、分页参数非法、必填字段缺失。
 * 因为请求尚未发送到平台，该异常的 {@code resultUnknown} 固定为 {@code false}。</p>
 */
public class StationOpenApiClientException extends StationOpenApiException {

    /**
     * 创建不带请求上下文的本地配置或参数异常。
     *
     * @param message 本地失败说明；进入异常对象前会统一脱敏
     */
    public StationOpenApiClientException(String message) {
        super(message);
    }

    /**
     * 创建带脱敏原因链的本地配置或参数异常。
     *
     * @param message 本地失败说明
     * @param cause 原始原因；进入异常对象前会统一脱敏
     */
    public StationOpenApiClientException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建携带请求上下文的本地配置或参数异常。
     *
     * @param message 本地失败说明
     * @param path 尚未发出的目标接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     */
    public StationOpenApiClientException(String message, String path, String requestId, String operation) {
        super(message, path, requestId, operation, null, false);
    }
}
