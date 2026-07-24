package com.deeprobotics.station.openapi.sdk.api;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraResetRequest;

/**
 * 摄像机和云台控制 API 分组。
 *
 * <p>摄像机控制会影响现场设备，SDK 按 DEVICE_CONTROL 操作处理且默认不重试。
 * 如果发生超时或连接中断，异常会带 {@code resultUnknown=true}；调用方应先确认设备状态，
 * 不要立即重复下发。</p>
 *
 * <p>摄像机流台账信息可通过 {@code inventory().pageStreams(...)} 查询，其中可能包含敏感的
 * {@code password} 字段。SDK 的字符串输出、异常和日志不会输出其明文值。</p>
 */
public interface CameraApi {

    /**
     * 控制摄像机云台、可见光镜头或红外镜头。
     *
     * <p>对应 {@code POST /remoteApi/cameraCtrl}。这是设备控制操作，默认不重试；如果发生
     * 超时或连接中断，异常会标记 {@code resultUnknown=true}，客户应先查询设备状态或等待事件确认。</p>
     *
     * @param request 摄像机控制请求，必须包含摄像机编码、控制类型和控制指令
     */
    void control(CameraControlRequest request);

    /**
     * 带单次调用选项的摄像机控制。
     *
     * <p>{@link RequestOptions} 可覆盖 requestId、超时或重试策略。当前摄像机控制按 DEVICE_CONTROL 处理，
     * 默认不重试；如果超时或连接中断，客户应先确认现场状态，不要基于同一业务意图立即重复下发。</p>
     *
     * @param request 摄像机控制请求，不能为 {@code null}
     * @param options 本次调用的 requestId 和超时等覆盖配置；传 {@code null} 时使用客户端默认配置
     */
    void control(CameraControlRequest request, RequestOptions options);

    /**
     * 复位摄像机云台。
     *
     * <p>使用 {@code POST /remoteApi/ptzReset}，并按设备控制操作处理。</p>
     *
     * @param request 云台复位请求，必须包含摄像机编码
     */
    void resetPtz(CameraResetRequest request);

    /**
     * 带单次调用选项的云台复位。
     *
     * @param request 云台复位请求，不能为 {@code null}
     * @param options 本次调用的 requestId 和超时等覆盖配置；传 {@code null} 时使用客户端默认配置
     */
    void resetPtz(CameraResetRequest request, RequestOptions options);
}
