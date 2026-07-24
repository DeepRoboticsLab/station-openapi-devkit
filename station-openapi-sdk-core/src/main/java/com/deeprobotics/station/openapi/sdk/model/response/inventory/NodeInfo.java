package com.deeprobotics.station.openapi.sdk.model.response.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 路网节点信息。
 *
 * <p>该模型描述路网中的节点，调用方可结合 {@link RoadMapInfo} 绘制路线拓扑或做选点展示。
 * 坐标字段保持平台返回的字符串形态。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NodeInfo(
        /**
         * 路网节点编码。
         */
        String nodeCode,
        /**
         * 路网节点名称。
         */
        String nodeName,
        /**
         * 关联四足狗编码。
         */
        String dogCode,
        /**
         * 楼层标识。
         */
        String floor,
        /**
         * 节点坐标字符串。
         */
        String coordinate) {
}
