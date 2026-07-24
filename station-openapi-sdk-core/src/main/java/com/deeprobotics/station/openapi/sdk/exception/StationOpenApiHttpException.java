package com.deeprobotics.station.openapi.sdk.exception;

/**
 * HTTP 层非 2xx 响应。
 *
 * <p>这类异常表示请求已经收到 HTTP 响应，但 status code 不在成功范围内。
 * {@code bodySnippet} 只应是经过长度限制和脱敏的响应片段，不能把完整响应体塞进异常。</p>
 */
public class StationOpenApiHttpException extends StationOpenApiException {

    /**
     * 创建不带原因链的 HTTP 非 2xx 异常。
     *
     * @param statusCode HTTP status code
     * @param path 失败接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param bodySnippet 已限制长度的响应体片段；进入异常对象前仍会统一脱敏
     */
    public StationOpenApiHttpException(int statusCode, String path, String requestId, String operation,
            String bodySnippet) {
        super("HTTP request failed: " + bodySnippet, path, requestId, operation, statusCode, false);
    }

    /**
     * 创建带脱敏原因链的 HTTP 非 2xx 异常。
     *
     * @param statusCode HTTP status code
     * @param path 失败接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param bodySnippet 已限制长度的响应体片段；进入异常对象前仍会统一脱敏
     * @param cause 读取或处理错误响应时的原始原因
     */
    public StationOpenApiHttpException(int statusCode, String path, String requestId, String operation,
            String bodySnippet, Throwable cause) {
        super("HTTP request failed: " + bodySnippet, path, requestId, operation, statusCode, false, cause);
    }
}
