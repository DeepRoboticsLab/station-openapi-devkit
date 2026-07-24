package com.deeprobotics.station.openapi.sdk.model.response.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 摄像机流媒体信息。
 *
 * <p>平台会返回摄像机连接和流媒体播放相关字段，其中
 * {@code password} 是真实响应字段，SDK 必须能反序列化，但 {@link #toString()} 永远不会输出明文密码。</p>
 *
 * <p>未知字段会被忽略，以兼容平台新增摄像机厂商、码流或播放参数。内置 {@link #toString()}
 * 只自动隐藏 {@code password}；用户名、IP、端口和拉流地址仍会输出，调用方若把这些连接信息也视为敏感数据，
 * 不应直接记录整个对象。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CameraStreamInfo(
        /**
         * 摄像机或视频流名称。
         */
        String name,
        /**
         * 摄像机或视频流编码。
         */
        String code,
        /**
         * 绑定设备编码，通常用于关联四足狗或其他现场设备。
         */
        String bindDeviceCode,
        /**
         * 摄像机厂商，SDK 保持平台原值。
         */
        String manufacturer,
        /**
         * 设备型号或设备模式。
         */
        String deviceMode,
        /**
         * 使用模式，具体码值以平台和现场配置为准。
         */
        String useMode,
        /**
         * 视频模式，SDK 保持平台原始编码或文本。
         */
        String videoMode,
        /**
         * 摄像机 IP 地址。
         */
        String ip,
        /**
         * 摄像机端口，按平台字符串透传。
         */
        String port,
        /**
         * 摄像机登录用户名。
         *
         * <p>该账号标识可能属于客户现场敏感配置；SDK 为排障保留其 {@code toString()} 输出，
         * 客户应按自身安全策略决定是否记录整个对象。</p>
         */
        String username,
        /**
         * 摄像机登录密码。
         *
         * <p>该字段可能用于客户系统二次集成播放器或网关，但必须按敏感信息处理：
         * 不要写入日志、异常或页面默认展示。SDK 的 {@link #toString()} 已固定脱敏。</p>
         */
        String password,
        /**
         * 摄像机通道号。
         */
        String channel,
        /**
         * 码流标识，例如主码流、子码流等平台原值。
         */
        String bitstream,
        /**
         * 播放或拉流地址。
         */
        String streamUrl) {

    @Override
    public String toString() {
        // password 是平台真实返回字段，但调试字符串只展示脱敏占位；其他连接元数据仍按字段值输出。
        return "CameraStreamInfo{"
                + "name='" + name + '\''
                + ", code='" + code + '\''
                + ", bindDeviceCode='" + bindDeviceCode + '\''
                + ", manufacturer='" + manufacturer + '\''
                + ", deviceMode='" + deviceMode + '\''
                + ", useMode='" + useMode + '\''
                + ", videoMode='" + videoMode + '\''
                + ", ip='" + ip + '\''
                + ", port='" + port + '\''
                + ", username='" + username + '\''
                + ", password=<redacted>"
                + ", channel='" + channel + '\''
                + ", bitstream='" + bitstream + '\''
                + ", streamUrl='" + streamUrl + '\''
                + '}';
    }
}
