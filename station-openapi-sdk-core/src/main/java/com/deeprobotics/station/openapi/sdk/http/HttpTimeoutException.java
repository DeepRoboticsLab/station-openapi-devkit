package com.deeprobotics.station.openapi.sdk.http;

import com.deeprobotics.station.openapi.sdk.exception.Redactor;

/**
 * transport 抛出的超时异常。
 *
 * <p>具体 transport 应把连接、读取或调用总时长超时转换为该异常。
 * executor 会根据操作意图判断是否重试以及是否设置 resultUnknown。</p>
 */
public class HttpTimeoutException extends RuntimeException {

    /**
     * 创建不带底层原因的超时异常。
     *
     * @param message 会先经过脱敏的错误说明
     */
    public HttpTimeoutException(String message) {
        super(Redactor.redact(message));
    }

    /**
     * 创建保留底层原因的超时异常。
     *
     * @param message 会先经过脱敏的错误说明
     * @param cause 连接、读取或调用总超时原因
     */
    public HttpTimeoutException(String message, Throwable cause) {
        super(Redactor.redact(message), cause);
    }
}
