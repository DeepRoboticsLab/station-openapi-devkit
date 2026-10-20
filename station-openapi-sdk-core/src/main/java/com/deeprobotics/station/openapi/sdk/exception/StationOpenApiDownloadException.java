package com.deeprobotics.station.openapi.sdk.exception;

/**
 * 文件下载失败异常。
 *
 * <p>用于下载响应无效或输出流写入失败。{@code resultUnknown=true} 表示调用方输出可能不完整，
 * 应丢弃不完整输出后重新下载。连接和超时失败仍使用对应的传输异常和超时异常。</p>
 */
public class StationOpenApiDownloadException extends StationOpenApiException {

    /**
     * 创建不带原因链的下载异常。
     *
     * @param message 下载失败说明
     * @param path 下载接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param resultUnknown 调用方的输出流是否可能已经收到部分字节
     */
    public StationOpenApiDownloadException(String message, String path, String requestId, String operation,
            boolean resultUnknown) {
        super(message, path, requestId, operation, null, resultUnknown);
    }

    /**
     * 创建带脱敏原因链的下载异常。
     *
     * @param message 下载失败说明
     * @param path 下载接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param resultUnknown 调用方的输出流是否可能已经收到部分字节
     * @param cause 原始下载或写入失败原因
     */
    public StationOpenApiDownloadException(String message, String path, String requestId, String operation,
            boolean resultUnknown, Throwable cause) {
        super(message, path, requestId, operation, null, resultUnknown, cause);
    }
}
