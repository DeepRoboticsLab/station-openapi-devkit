package com.deeprobotics.station.openapi.sdk.exception;

/**
 * 文件下载失败异常。
 *
 * <p>下载的失败面比普通 JSON 请求多：远端可能返回 JSON 错误体，也可能边下载边中断，
 * 还可能在最终写入客户 OutputStream 时失败。写入客户输出流失败时设置 {@code resultUnknown=true}，
 * 因为调用方可能已经收到部分字节。</p>
 */
public class StationOpenApiDownloadException extends StationOpenApiException {

    /**
     * 创建不带原因链的下载异常。
     *
     * @param message 下载失败说明
     * @param path 下载接口 path
     * @param requestId 请求链路 ID
     * @param operation SDK 操作标识
     * @param resultUnknown 客户输出流是否可能已经收到部分字节
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
     * @param resultUnknown 客户输出流是否可能已经收到部分字节
     * @param cause 原始下载或写入失败原因
     */
    public StationOpenApiDownloadException(String message, String path, String requestId, String operation,
            boolean resultUnknown, Throwable cause) {
        super(message, path, requestId, operation, null, resultUnknown, cause);
    }
}
