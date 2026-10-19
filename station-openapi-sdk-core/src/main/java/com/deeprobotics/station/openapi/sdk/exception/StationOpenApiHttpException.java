package com.deeprobotics.station.openapi.sdk.exception;

/**
 * HTTP 层非 2xx 响应。
 *
 * <p>这类异常表示请求已经收到 HTTP 响应，但 status code 不在成功范围内。
 * 只保留状态码和请求上下文，不保留响应体；错误页或响应片段可能包含没有字段名标记的凭据。</p>
 */
public class StationOpenApiHttpException extends StationOpenApiException {

    /**
     * 创建不带原因链的 HTTP 非 2xx 异常。
     *
     * @param statusCode HTTP status code
     * @param path 失败接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param bodySnippet 为兼容原构造器保留的参数；内容被忽略，不进入异常
     */
    public StationOpenApiHttpException(int statusCode, String path, String requestId, String operation,
            String bodySnippet) {
        super("HTTP request failed", path, requestId, operation, statusCode, false);
    }

    /**
     * 创建带脱敏原因链的 HTTP 非 2xx 异常。
     *
     * @param statusCode HTTP status code
     * @param path 失败接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param bodySnippet 为兼容原构造器保留的参数；内容被忽略，不进入异常
     * @param cause 读取或处理错误响应时的原始原因
     */
    public StationOpenApiHttpException(int statusCode, String path, String requestId, String operation,
            String bodySnippet, Throwable cause) {
        super("HTTP request failed", path, requestId, operation, statusCode, false, cause);
    }
}
