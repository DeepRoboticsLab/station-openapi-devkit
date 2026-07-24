package com.deeprobotics.station.openapi.sdk.model.response.inventory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 检修区域信息。
 *
 * <p>该模型描述地图上的维保/检修区域范围、日期窗口、下发状态和关联点位。
 * 坐标按平台字符串透传；开始和结束日期固定为 {@code yyyy-MM-dd} 字符串，
 * 不包含时分秒或时区。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MaintainAreaInfo(
        /**
         * 维保区域编码。
         */
        String maintainAreaCode,
        /**
         * 区域起点 X 坐标。
         */
        String startX,
        /**
         * 区域起点 Y 坐标。
         */
        String startY,
        /**
         * 区域终点 X 坐标。
         */
        String endX,
        /**
         * 区域终点 Y 坐标。
         */
        String endY,
        /**
         * 检修开始日期，格式为 {@code yyyy-MM-dd}。
         */
        String startTime,
        /**
         * 检修结束日期，格式为 {@code yyyy-MM-dd}。
         */
        String endTime,
        /**
         * 维修或维保描述。
         */
        String repairDesc,
        /**
         * 关联四足狗编码。
         */
        String dogCode,
        /**
         * 是否已经下发到四足狗：{@code 0} 未下发，{@code 1} 已下发。
         */
        Integer issued,
        /**
         * 关联巡检点编码集合，平台以英文逗号分隔字符串返回。
         */
        String pointCodes,
        /**
         * 关联巡检点数量。
         */
        Integer pointCount,
        /**
         * 楼层标识。
         */
        String floor,
        /**
         * 检修区域模式：{@code 1} 框选区域，{@code 2} 点位集合。
         */
        Integer mode) {
}
