package com.deeprobotics.station.openapi.sample.service;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraResetRequest;
import org.springframework.stereotype.Service;

/**
 * 直接展示 {@code client.camera()} 分组的摄像机与云台控制调用。
 *
 * <p>Service 不封装额外控制协议，也不为可能影响现场设备的操作增加自动重试。</p>
 */
@Service
public class CameraSampleService {

    private final StationOpenApiClient client;

    public CameraSampleService(StationOpenApiClient client) {
        this.client = client;
    }

    public void control(CameraControlRequest request, RequestOptions options) {
        client.camera().control(request, options);
    }

    public void resetPtz(CameraResetRequest request, RequestOptions options) {
        client.camera().resetPtz(request, options);
    }
}
