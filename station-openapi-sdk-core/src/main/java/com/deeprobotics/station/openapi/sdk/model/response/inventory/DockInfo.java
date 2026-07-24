package com.deeprobotics.station.openapi.sdk.model.response.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 停靠点信息。
 *
 * <p>该模型描述路网中的导航停靠点或机巢点，常用于地图展示、巡检点靠近和任务路线规划。
 * 是否具备充电能力不能仅由本模型判断；坐标字段保持平台返回的字符串形态。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DockInfo(
        /**
         * 停靠点或机巢编码。
         */
        String dockCode,
        /**
         * 关联四足狗编码。
         */
        String dogCode,
        /**
         * 所属路网边编码。
         */
        String edgeCode,
        /**
         * 楼层标识。
         */
        String floor,
        /**
         * 停靠点坐标字符串。
         */
        String coordinate,
        /**
         * 优先级，按平台字符串透传。
         */
        String priority,
        /**
         * 停靠点或机巢名称。
         */
        String dockName) {
}
