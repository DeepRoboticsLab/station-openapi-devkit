package com.deeprobotics.station.openapi.sdk.model.response.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 路网边信息。
 *
 * <p>该模型描述路网中一条边的起止节点、控制点和禁行状态。调用方可结合 {@link NodeInfo}
 * 绘制完整路线拓扑。坐标字段按平台字符串透传，不在 SDK 内拆分或换算单位。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RoadMapInfo(
        /**
         * 关联四足狗编码。
         */
        String dogCode,
        /**
         * 路网边编码。
         */
        String edgeCode,
        /**
         * 路网边起点节点编码。
         */
        String edgeStartNode,
        /**
         * 路网边终点节点编码。
         */
        String edgeEndNode,
        /**
         * 起点坐标字符串。
         */
        String startCoordinate,
        /**
         * 终点坐标字符串。
         */
        String endCoordinate,
        /**
         * 贝塞尔或路径控制点 1，按平台字符串透传。
         */
        String controlPoint1,
        /**
         * 贝塞尔或路径控制点 2，按平台字符串透传。
         */
        String controlPoint2,
        /**
         * 楼层标识。
         */
        String floor,
        /**
         * 选点下发禁用状态。
         *
         * <p>{@code true} 表示该边不能用于 {@code dog().setPosition(...)} 选点下发；
         * 调用方仍可在地图上展示该边，但应明确标记为不可选。</p>
         */
        Boolean moveDisable) {
}
