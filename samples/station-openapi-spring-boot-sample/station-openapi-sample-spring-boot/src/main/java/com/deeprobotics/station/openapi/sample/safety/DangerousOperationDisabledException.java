package com.deeprobotics.station.openapi.sample.safety;

/**
 * 危险操作开关关闭时由 Sample 本地抛出的异常。
 *
 * <p>该异常表示请求尚未调用平台，不是 Station OpenAPI 返回的业务错误。</p>
 */
public class DangerousOperationDisabledException extends RuntimeException {

    public DangerousOperationDisabledException() {
        super("Dangerous Sample operations are disabled");
    }
}
