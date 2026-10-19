package com.deeprobotics.station.openapi.sdk.exception;

/**
 * 请求超时异常。
 *
 * <p>超时不等同于业务失败。对于任务下发、设备控制、业务写入等危险操作，
 * 平台或设备可能已经收到命令，只是 SDK 没等到响应，因此 {@code resultUnknown} 必须保留给调用方判断后续动作。</p>
 *
 * <p>下载中途失败时，{@code resultUnknown=true} 表示输出可能不完整，应丢弃后重新下载。</p>
 */
public class StationOpenApiTimeoutException extends StationOpenApiException {

    /**
     * 创建不带原因链的请求超时异常。
     *
     * @param message 超时说明
     * @param path 超时接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param resultUnknown 执行结果是否未确认，或下载输出是否可能不完整
     */
    public StationOpenApiTimeoutException(String message, String path, String requestId, String operation,
            boolean resultUnknown) {
        super(message, path, requestId, operation, null, resultUnknown);
    }

    /**
     * 创建带脱敏原因链的请求超时异常。
     *
     * @param message 超时说明
     * @param path 超时接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param resultUnknown 执行结果是否未确认，或下载输出是否可能不完整
     * @param cause transport 抛出的原始超时原因
     */
    public StationOpenApiTimeoutException(String message, String path, String requestId, String operation,
            boolean resultUnknown, Throwable cause) {
        super(message, path, requestId, operation, null, resultUnknown, cause);
    }
}
