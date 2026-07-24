package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;

/**
 * 客户业务 API 进入内部执行链前的参数边界。
 *
 * <p>仅统一公共方法的本地输入错误；内部装配构造器仍使用 JDK 非空检查区分 SDK 编程错误。</p>
 */
final class ClientInputs {

    private ClientInputs() {
    }

    static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return value;
    }
}
