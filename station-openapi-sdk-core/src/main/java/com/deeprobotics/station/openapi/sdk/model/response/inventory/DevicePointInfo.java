package com.deeprobotics.station.openapi.sdk.model.response.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 巡检点基础信息。
 *
 * <p>该模型描述巡检点和设备、部件、区域、楼层之间的关系，常用于生成巡检点列表、地图标注或结果关联。
 * 字段保持平台原始类型和命名，未知字段会被忽略。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DevicePointInfo(
        /**
         * 区域编码。
         */
        String areaCode,
        /**
         * 区域名称。
         */
        String areaName,
        /**
         * 间隔编码。
         */
        String intervalCode,
        /**
         * 间隔名称。
         */
        String intervalName,
        /**
         * 设备编码。
         */
        String deviceCode,
        /**
         * 设备名称。
         */
        String deviceName,
        /**
         * 部件编码。
         */
        String componentCode,
        /**
         * 部件名称。
         */
        String componentName,
        /**
         * 巡检点编码，是巡检结果和点位配置的主要关联字段。
         */
        String pointCode,
        /**
         * 巡检点名称。
         */
        String pointName,
        /**
         * 设备类型，SDK 保持平台原值。
         */
        String deviceType,
        /**
         * 识别类型，SDK 不在本地解释枚举含义。
         */
        String recognitionType,
        /**
         * 相别或设备相位信息。
         */
        String phase,
        /**
         * 相别分组标识。
         */
        String phaseGroupId,
        /**
         * 关联四足狗编码。
         */
        String dogCode,
        /**
         * 关联机巢或停靠点编码。
         */
        String dockCode,
        /**
         * 楼层标识。
         */
        String floor) {
}
