package com.deeprobotics.station.openapi.sample.web.view;

import com.deeprobotics.station.openapi.sdk.model.response.inventory.CameraStreamInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 面向 Sample 响应和 Swagger 的摄像机安全视图。
 *
 * <p>SDK 模型可能包含摄像机密码，本视图刻意不声明该字段，避免序列化结果和 OpenAPI schema 泄露凭证。</p>
 */
@Schema(description = "不包含摄像机密码的安全流媒体视图 / Safe camera stream view without password")
public record CameraStreamView(
        String name,
        String code,
        String bindDeviceCode,
        String manufacturer,
        String deviceMode,
        String useMode,
        String videoMode,
        String ip,
        String port,
        String username,
        String channel,
        String bitstream,
        String streamUrl) {

    /**
     * 从 SDK 模型复制允许公开的字段；密码字段不得进入 Sample 响应。
     */
    public static CameraStreamView from(CameraStreamInfo source) {
        return new CameraStreamView(
                source.name(),
                source.code(),
                source.bindDeviceCode(),
                source.manufacturer(),
                source.deviceMode(),
                source.useMode(),
                source.videoMode(),
                source.ip(),
                source.port(),
                source.username(),
                source.channel(),
                source.bitstream(),
                source.streamUrl());
    }
}
