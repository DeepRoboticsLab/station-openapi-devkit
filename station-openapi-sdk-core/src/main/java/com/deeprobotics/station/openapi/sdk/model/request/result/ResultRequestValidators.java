package com.deeprobotics.station.openapi.sdk.model.request.result;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;

/**
 * 巡检结果请求模型的包内校验工具。
 *
 * <p>校验必填文本，以及资源下载 {@code type} 的取值范围。</p>
 */
final class ResultRequestValidators {

    private ResultRequestValidators() {
    }

    /**
     * 校验必填文本字段并去除首尾空白。
     *
     * <p>用于 {@code taskExecuteCode} 和 {@code filePath}。校验失败时不会发送 HTTP 请求。</p>
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
     * <p>仅接受 1（地图文件）和 2（结果资源），不检查路径实际对应哪种资源。</p>
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
