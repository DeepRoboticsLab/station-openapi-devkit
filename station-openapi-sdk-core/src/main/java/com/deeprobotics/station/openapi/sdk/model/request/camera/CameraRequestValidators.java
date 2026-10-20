package com.deeprobotics.station.openapi.sdk.model.request.camera;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;

/**
 * 摄像机请求模型的本地校验工具。
 *
 * <p>SDK 在发出 HTTP 前拦截明显错误，使调用方在进入现场设备链路前即可发现参数问题。
 * 这里保持包内可见，不把它设计成公共校验框架。</p>
 */
final class CameraRequestValidators {

    private CameraRequestValidators() {
    }

    /**
     * 校验并规范化必填文本字段。
     *
     * <p>返回 trim 后的值，保证请求体和签名源使用同一个规范化结果。</p>
     */
    static String requireText(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 校验必填整数字段。
     *
     * <p>摄像机控制的 {@code ctrlType/command} 都是平台必填字段，缺失时直接抛本地参数异常。</p>
     */
    static Integer requireInteger(String fieldName, Integer value) {
        if (value == null) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return value;
    }

    /**
     * 校验整数范围。
     *
     * <p>{@code value} 这类可选字段允许为 null；只有调用方显式传值时才检查范围。</p>
     */
    static void requireRange(String fieldName, Integer value, int min, int max) {
        if (value != null && (value < min || value > max)) {
            throw new StationOpenApiClientException(fieldName + " must be between " + min + " and " + max);
        }
    }
}
