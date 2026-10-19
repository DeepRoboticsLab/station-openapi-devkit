package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.api.CameraApi;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraResetRequest;
import java.util.Objects;

/**
 * 摄像机 API 默认实现。
 *
 * <p>控制和复位均会改变设备状态，不自动重试；断连后标记 {@code resultUnknown=true}。
 * 成功响应的 data 可为空。</p>
 */
public final class DefaultCameraApi implements CameraApi {

    private static final EndpointDefinition CONTROL =
            EndpointDefinition.of("camera.control", "/remoteApi/cameraCtrl", OperationIntent.DEVICE_CONTROL);
    private static final EndpointDefinition RESET_PTZ =
            EndpointDefinition.of("camera.resetPtz", "/remoteApi/ptzReset", OperationIntent.DEVICE_CONTROL);

    private final OpenApiExecutor executor;

    /**
     * 创建摄像机 API 实现。
     *
     * @param executor 已完成客户端级配置的统一执行器
     */
    public DefaultCameraApi(OpenApiExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor is required");
    }

    @Override
    public void control(CameraControlRequest request) {
        control(request, null);
    }

    @Override
    public void control(CameraControlRequest request, RequestOptions options) {
        CameraControlRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        executor.execute(CONTROL, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }

    @Override
    public void resetPtz(CameraResetRequest request) {
        resetPtz(request, null);
    }

    @Override
    public void resetPtz(CameraResetRequest request, RequestOptions options) {
        CameraResetRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        executor.execute(RESET_PTZ, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }
}
