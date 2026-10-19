package com.deeprobotics.station.openapi.sample.web.view;

import com.deeprobotics.station.openapi.sdk.model.response.inventory.CameraStreamInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;

/**
 * 面向 Sample 响应和 Swagger 的摄像机安全视图。
 *
 * <p>SDK 模型可能包含摄像机密码，本视图不声明该字段。流地址若带账号、查询参数、片段或无法解析，
 * 也不展示，防止 URL 内的密码或访问令牌经响应和日志泄露。</p>
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
        @Schema(description = "不含凭据的流地址；含账号或 URL 参数时省略 / "
                + "Stream URL without credentials; omitted when it contains user-info or URL parameters")
        String streamUrl) {

    /**
     * 构造安全展示快照；SDK 原始流地址仍可在受控的服务端集成中读取。
     *
     * @param name 摄像机名称
     * @param code 摄像机编码
     * @param bindDeviceCode 绑定设备编码
     * @param manufacturer 厂商
     * @param deviceMode 设备型号
     * @param useMode 使用模式
     * @param videoMode 视频模式
     * @param ip 摄像机地址
     * @param port 摄像机端口
     * @param username 摄像机账号标识，不包含密码
     * @param channel 通道号
     * @param bitstream 码流标识
     * @param streamUrl 待展示地址；含潜在凭据或无法解析时转为 null
     */
    public CameraStreamView {
        streamUrl = displayableStreamUrl(streamUrl);
    }

    private static String displayableStreamUrl(String value) {
        if (value == null) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null ? value : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * 从 SDK 模型复制允许公开的字段；密码字段不得进入 Sample 响应。
     *
     * @param source 平台返回的摄像机信息
     * @return 已去除密码和潜在敏感地址的展示视图
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
