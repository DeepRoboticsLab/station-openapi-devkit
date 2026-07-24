package com.deeprobotics.station.openapi.sdk.model.request.result;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;

/**
 * 巡检结果请求模型的包内校验工具。
 *
 * <p>结果分组包含普通分页查询和二进制资源下载。分页查询只需要必填文本校验；
 * 下载请求额外要求 {@code type} 必须是平台定义的 1 或 2。集中在这里可避免各请求模型复制异常文案和范围判断。</p>
 */
final class ResultRequestValidators {

    private ResultRequestValidators() {
    }

    /**
     * 校验必填文本字段并去除首尾空白。
     *
     * <p>用于 {@code taskExecuteCode} 和 {@code filePath}。校验失败时不会发送 HTTP 请求；Path 下载重载
     * 即使已经创建本次调用的临时文件，也会在异常路径中将其清理。</p>
     */
    static String requireText(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 校验资源下载类型。
     *
     * <p>平台请求字段 {@code type} 只允许 1 和 2。SDK 在本地拒绝其他值，
     * 避免客户误把巡检结果文件按地图文件下载，或发送尚未支持的类型。</p>
     */
    static Integer requireResourceType(Integer type) {
        if (type == null) {
            throw new StationOpenApiClientException("type is required");
        }
        if (type != 1 && type != 2) {
            throw new StationOpenApiClientException("type must be 1 or 2");
        }
        return type;
    }
}
