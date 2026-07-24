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
 * <p>本类只负责将 public API 方法映射到平台 API path。鉴权、JSON 序列化、统一响应解包、
 * 重试和异常转换均由 {@link OpenApiExecutor} 处理。</p>
 */
public final class DefaultCameraApi implements CameraApi {

    /**
     * 摄像机控制 endpoint。
     *
     * <p>{@link OperationIntent#DEVICE_CONTROL} 会让 {@link OpenApiExecutor} 使用设备控制类的默认策略：
     * 不做自动重试，连接/读取超时后把异常标记为 {@code resultUnknown=true}。这里不要改成 READ_ONLY，
     * 否则客户可能在网络抖动时重复下发云台或镜头动作。</p>
     */
    private static final EndpointDefinition CONTROL =
            EndpointDefinition.of("camera.control", "/remoteApi/cameraCtrl", OperationIntent.DEVICE_CONTROL);
    /**
     * 云台复位 endpoint。
     *
     * <p>平台使用 POST 暴露 {@code /remoteApi/ptzReset}。复位同样会改变现场摄像机状态，
     * 因此必须和普通控制一样按 DEVICE_CONTROL 处理。</p>
     */
    private static final EndpointDefinition RESET_PTZ =
            EndpointDefinition.of("camera.resetPtz", "/remoteApi/ptzReset", OperationIntent.DEVICE_CONTROL);

    /**
     * 统一执行器，负责鉴权、签名、JSON 序列化、响应解包、重试和异常转换。
     */
    private final OpenApiExecutor executor;

    /**
     * 创建摄像机 API 实现。
     *
     * <p>构造阶段立即校验 executor，避免业务方法调用到一半才出现难以定位的空指针异常。</p>
     *
     * @param executor 已完成客户端级配置的统一执行器
     */
    public DefaultCameraApi(OpenApiExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor is required");
    }

    @Override
    public void control(CameraControlRequest request) {
        // 无 RequestOptions 的重载统一委托给完整方法，确保两种入口走同一套校验和执行链路。
        control(request, null);
    }

    @Override
    public void control(CameraControlRequest request, RequestOptions options) {
        CameraControlRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        /*
         * 平台响应可能不携带 data，SDK 公共方法只关心成功或失败。
         * TypeRef<ApiResponse<Object>> 保留统一 R 解包能力，同时避免把无业务意义的 Object 返回给客户。
         */
        executor.execute(CONTROL, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }

    @Override
    public void resetPtz(CameraResetRequest request) {
        // 复位和控制一样走完整方法，保持 RequestOptions 覆盖逻辑一致。
        resetPtz(request, null);
    }

    @Override
    public void resetPtz(CameraResetRequest request, RequestOptions options) {
        CameraResetRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        // 云台复位同样只需要判断平台响应是否成功，不向调用方暴露空 data。
        executor.execute(RESET_PTZ, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }
}
