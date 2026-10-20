package com.deeprobotics.station.openapi.sdk.model.response.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 四足狗台账信息。
 *
 * <p>该模型只描述调用方需要展示或关联的基础台账字段，未知字段会被 Jackson 忽略，
 * 以便平台增加字段时已有 SDK 仍能解析响应。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DogInfo(
        /**
         * 四足狗名称，通常用于页面展示。
         */
        String dogName,
        /**
         * 四足狗唯一编码，是后续状态查询、控制、地图和路网查询的主要关联键。
         */
        String dogCode,
        /**
         * 设备型号或模式，例如现场常见的 X30、M20 等平台原始值。
         */
        String deviceMode,
        /**
         * 狗类型编码，SDK 不在本地解释枚举含义，保持平台原值。
         */
        String dogTypeCode,
        /**
         * 生产编号或出厂编码，用于资产系统做设备追溯。
         */
        String productionCode,
        /**
         * 设备网络 IP，可能随现场网络配置变化。
         */
        String ip,
        /**
         * 设备网络端口，按平台字符串透传。
         */
        String port,
        /**
         * 区域/楼层编码集合。
         *
         * <p>多个值使用英文逗号分隔，调用方不应把它假定为单个固定楼层。</p>
         */
        String floor) {
}
