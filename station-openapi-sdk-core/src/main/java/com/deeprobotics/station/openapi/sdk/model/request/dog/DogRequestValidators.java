package com.deeprobotics.station.openapi.sdk.model.request.dog;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;

/**
 * 四足狗请求模型的本地校验工具。
 *
 * <p>这些方法只服务 dog 请求包，不放到公共 API，避免客户误以为 SDK 提供通用校验框架。
 * 这里抛出的异常属于客户入参错误，发生在 HTTP 请求发出之前，便于客户在本地快速定位问题。</p>
 */
final class DogRequestValidators {

    private DogRequestValidators() {
    }

    static String requireText(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        // SDK 统一 trim 并校验非空，避免签名源混入无意义的首尾空格。
        return value.trim();
    }

    static Integer requireInteger(String fieldName, Integer value) {
        if (value == null) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return value;
    }
}
