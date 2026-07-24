package com.deeprobotics.station.openapi.sdk.model.response.dog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 四足狗实时坐标。
 *
 * <p>{@code /remoteApi/dogPos} 响应的 {@code data} 是 JSON 字符串，SDK 在核心层把它解析成本模型。
 * 这里只暴露当前稳定字段，其他字段按未知字段处理。</p>
 *
 * <p>坐标和角度字段保持 {@link String}，不转换为数值类型。平台默认返回厘米制
 * {@code x/y/z}；如果启用了像素坐标配置，{@code x/y} 会进一步换算为地图像素，
 * {@code z} 仍保留厘米值。SDK 无法从响应本身判断现场是否启用了该配置，因此不做单位换算。</p>
 *
 * <p>{@code time} 也按平台字符串透传。它可能包含毫秒，例如 {@code 2026-02-26 20:24:32.932}，
 * 不强制转换为 {@code LocalDateTime} 可以避免不同现场时间精度不一致时解析失败。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DogPosition(
        /**
         * 四足狗编码。
         *
         * <p>来自位置响应 JSON 的 {@code dogCode} 字段，用于把坐标与台账、任务或状态接口返回的四足狗关联起来。
         * SDK 不从请求对象回填该值，而是使用平台响应值。</p>
         */
        String dogCode,
        /**
         * 四足狗名称。
         *
         * <p>来自位置响应 JSON 的 {@code dogName} 字段，主要用于调用方界面展示。该名称可能随平台台账修改，
         * 客户如果需要稳定关联，应优先使用 {@link #dogCode()}。</p>
         */
        String dogName,
        /**
         * X 坐标文本。
         *
         * <p>平台默认返回厘米；启用像素坐标配置后返回地图像素。字段保持字符串，客户应结合平台配置
         * 解释单位和坐标系。</p>
         */
        String x,
        /**
         * Y 坐标文本。
         *
         * <p>与 {@link #x()} 使用同一现场坐标系，默认是厘米，启用像素坐标配置后是地图像素。
         * SDK 不做单位换算、四舍五入或合法范围校验。</p>
         */
        String y,
        /**
         * Z 坐标文本。
         *
         * <p>平台默认返回厘米值；像素坐标配置只转换 {@code x/y}，不转换 {@code z}。
         * 二维场景下该字段也可能为零值或缺失，SDK 保留服务端原始文本。</p>
         */
        String z,
        /**
         * 朝向角文本。
         *
         * <p>对应位置响应 JSON 的 {@code yaw} 字段。平台不对四足狗上报值做单位转换；接口契约按弧度说明。
         * SDK 不转换成角度或数值，以免改变精度或掩盖现场协议差异。</p>
         */
        String yaw,
        /**
         * 坐标上报时间文本。
         *
         * <p>来自四足狗坐标上报，常见格式为 {@code yyyy-MM-dd HH:mm:ss.SSS}。不同现场时间精度可能不同，
         * 因此 SDK 按字符串透传，不强制解析为 Java 时间类型。</p>
         */
        String time) {
}
