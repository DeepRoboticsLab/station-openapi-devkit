package com.deeprobotics.station.openapi.sdk.exception;

/**
 * 平台业务失败响应。
 *
 * <p>HTTP status 可能是 200，但 {@code success=false} 或 {@code code!=200}。
 * 这种情况说明平台明确拒绝或处理失败，SDK 不应自动重试。异常会保留平台返回的 {@code msg}，
 * 是否直接展示应由调用方的国际化和错误提示策略决定。</p>
 */
public class StationOpenApiBusinessException extends StationOpenApiException {

    /**
     * 创建平台明确返回的业务失败异常。
     *
     * @param code 平台响应 {@code code}
     * @param message 平台响应 {@code msg}
     * @param path 失败接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     */
    public StationOpenApiBusinessException(Integer code, String message, String path, String requestId,
            String operation) {
        super("OpenAPI business error: " + message, path, requestId, operation, code, false);
    }

    /**
     * 创建带脱敏原因链的平台业务失败异常。
     *
     * @param code 平台响应 {@code code}
     * @param message 平台响应 {@code msg}
     * @param path 失败接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param cause 解析或映射业务失败时的原始原因
     */
    public StationOpenApiBusinessException(Integer code, String message, String path, String requestId,
            String operation, Throwable cause) {
        super("OpenAPI business error: " + message, path, requestId, operation, code, false, cause);
    }
}
