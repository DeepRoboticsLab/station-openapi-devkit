package com.deeprobotics.station.openapi.sdk.exception;

/**
 * SDK 鉴权配置或鉴权加工异常。
 *
 * <p>例如签名模式缺少凭证、nonce 生成失败或传入了错误鉴权模式。异常消息仍必须脱敏，
 * 因为鉴权失败最容易携带 secretKey、token 或 sign。</p>
 */
public class StationOpenApiAuthException extends StationOpenApiException {

    /**
     * 创建不带请求上下文的鉴权异常。
     *
     * @param message 鉴权失败说明；进入异常对象前会统一脱敏
     */
    public StationOpenApiAuthException(String message) {
        super(message);
    }

    /**
     * 创建带脱敏原因链的鉴权异常。
     *
     * @param message 鉴权失败说明
     * @param cause 原始原因；进入异常对象前会统一脱敏
     */
    public StationOpenApiAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建携带请求上下文的鉴权异常。
     *
     * @param message 鉴权失败说明
     * @param path 失败接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param code 平台鉴权业务 code 或 HTTP status
     */
    public StationOpenApiAuthException(String message, String path, String requestId, String operation, Integer code) {
        super(message, path, requestId, operation, code, false);
    }
}
