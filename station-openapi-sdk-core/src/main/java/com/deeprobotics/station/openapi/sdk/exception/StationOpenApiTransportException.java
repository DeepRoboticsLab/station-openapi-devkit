package com.deeprobotics.station.openapi.sdk.exception;

/**
 * HTTP transport 层异常。
 *
 * <p>典型原因包括 DNS、连接建立失败、TLS 握手失败、连接被重置等。
 * 对控制/写入操作，连接中断同样可能出现结果未知，需要通过 {@code resultUnknown} 提醒客户确认状态。</p>
 *
 * <p>下载中途失败时，{@code resultUnknown=true} 表示输出可能不完整，应丢弃后重新下载。</p>
 */
public class StationOpenApiTransportException extends StationOpenApiException {

    /**
     * 创建不带原因链的 transport 异常。
     *
     * @param message 网络传输失败说明
     * @param path 失败接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param resultUnknown 执行结果是否未确认，或下载输出是否可能不完整
     */
    public StationOpenApiTransportException(String message, String path, String requestId, String operation,
            boolean resultUnknown) {
        super(message, path, requestId, operation, null, resultUnknown);
    }

    /**
     * 创建带脱敏原因链的 transport 异常。
     *
     * @param message 网络传输失败说明
     * @param path 失败接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param resultUnknown 执行结果是否未确认，或下载输出是否可能不完整
     * @param cause transport 抛出的原始网络失败原因
     */
    public StationOpenApiTransportException(String message, String path, String requestId, String operation,
            boolean resultUnknown, Throwable cause) {
        super(message, path, requestId, operation, null, resultUnknown, cause);
    }
}
