package com.deeprobotics.station.openapi.sdk.http;

import com.deeprobotics.station.openapi.sdk.exception.Redactor;

/**
 * transport 抛出的连接类异常。
 *
 * <p>具体 transport 应把 DNS、TLS、连接重置等非 HTTP status 失败转换为该异常，
 * executor 再根据操作意图包装为 {@code StationOpenApiTransportException}。</p>
 */
public class HttpConnectionException extends RuntimeException {

    /**
     * 创建不带底层原因的连接异常。
     *
     * @param message 会先经过脱敏的错误说明
     */
    public HttpConnectionException(String message) {
        super(Redactor.redact(message));
    }

    /**
     * 创建保留底层原因的连接异常。
     *
     * @param message 会先经过脱敏的错误说明
     * @param cause DNS、TLS、断连等底层原因
     */
    public HttpConnectionException(String message, Throwable cause) {
        super(Redactor.redact(message), cause);
    }
}
