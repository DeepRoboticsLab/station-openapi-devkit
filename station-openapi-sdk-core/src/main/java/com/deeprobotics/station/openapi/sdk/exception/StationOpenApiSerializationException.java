package com.deeprobotics.station.openapi.sdk.exception;

/**
 * JSON 序列化或反序列化异常。
 *
 * <p>请求序列化失败表示请求模型生成了 JSON codec 无法处理的值；响应反序列化失败表示响应体不是
 * 目标类型兼容的 JSON，或平台响应不符合 SDK wire contract。异常上下文应包含
 * path/requestId/operation 便于定位。</p>
 */
public class StationOpenApiSerializationException extends StationOpenApiException {

    /**
     * 创建不带请求上下文的 JSON 异常。
     *
     * @param message 序列化或反序列化失败说明
     */
    public StationOpenApiSerializationException(String message) {
        super(message);
    }

    /**
     * 创建带脱敏原因链但不带请求上下文的 JSON 异常。
     *
     * @param message 序列化或反序列化失败说明
     * @param cause JSON codec 抛出的原始原因
     */
    public StationOpenApiSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建携带请求上下文和脱敏原因链的 JSON 异常。
     *
     * @param message 序列化或反序列化失败说明
     * @param path 目标接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param cause JSON codec 抛出的原始原因
     */
    public StationOpenApiSerializationException(String message, String path, String requestId, String operation,
            Throwable cause) {
        super(message, path, requestId, operation, null, false, cause);
    }
}
