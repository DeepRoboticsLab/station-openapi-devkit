package com.deeprobotics.station.openapi.sdk.model.response.tasktemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * 任务模板信息。
 *
 * <p>用于任务模板详情和分页列表。未知字段会被忽略，以便平台新增展示字段时已有 SDK 仍能解析响应。</p>
 *
 * <p>该响应模型按平台字段原样建模：枚举和码表字段保持 String/Integer，计划说明
 * {@code planTimeContent} 可直接展示，不建议反向解析。时间字段中，平台返回完整日期时间的字段使用
 * {@link LocalDateTime}，每日执行时间这类仅有时分秒的字段保持字符串，避免不同平台返回格式细节导致解析失败。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskTemplateInfo(
        /**
         * 任务模板编码。启停、删除、查询详情和后续任务下发都依赖该编码。
         */
        String taskCode,
        /**
         * 任务模板名称。
         */
        String taskName,
        /**
         * 执行类型：1=立即，2=定时，3=周期，4=间隔。
         */
        Integer executeType,
        /**
         * 巡检类型，SDK 按平台原始字符串透传。
         */
        String patrolType,
        /**
         * 巡检点编码集合，平台返回英文逗号字符串。
         */
        String pointCodes,
        /**
         * 巡检点数量。
         */
        Integer pointCount,
        /**
         * 执行四足狗编码集合，平台返回英文逗号字符串。
         */
        String dogCode,
        /**
         * 优先级：{@code 1} 最低、{@code 4} 最高，{@code 2/3} 为中间等级。
         */
        Integer priority,
        /**
         * 任务结束动作：{@code "1"} 自动充电、{@code "2"} 原地待命。
         */
        String endAction,
        /**
         * 设备等级。
         */
        Integer deviceLevel,
        /**
         * 模板创建时间。
         */
        LocalDateTime createTime,
        /**
         * 是否启用：{@code "1"} 启用、{@code "0"} 停用。详情返回字符串，启停请求使用整数。
         */
        String isEnable,
        /**
         * 定时任务开始时间。
         */
        LocalDateTime fixedStartTime,
        /**
         * 周期月份，英文逗号字符串。
         */
        String cycleMonth,
        /**
         * 周期星期，英文逗号字符串。
         */
        String cycleWeek,
        /**
         * 周期任务每日执行时间，平台返回字符串。
         */
        String cycleExecuteTime,
        /**
         * 周期任务生效开始时间。
         */
        LocalDateTime cycleStartTime,
        /**
         * 周期任务生效结束时间。
         */
        LocalDateTime cycleEndTime,
        /**
         * 间隔执行数量。
         */
        Integer intervalNumber,
        /**
         * 间隔执行类型：1=小时，2=天，3=分钟。
         */
        Integer intervalType,
        /**
         * 间隔任务参考执行时间。
         */
        String intervalExecuteTime,
        /**
         * 间隔任务生效开始时间。
         */
        LocalDateTime intervalStartTime,
        /**
         * 间隔任务生效结束时间。
         */
        LocalDateTime intervalEndTime,
        /**
         * 整体不可用开始时间。
         */
        LocalDateTime invalidStartTime,
        /**
         * 整体不可用结束时间。
         */
        LocalDateTime invalidEndTime,
        /**
         * 每天不可用开始时间。
         */
        String invalidDailyStartTime,
        /**
         * 每天不可用结束时间。
         */
        String invalidDailyEndTime,
        /**
         * 是否可重复下发：{@code "0"} 不可下发、{@code "1"} 可下发。
         */
        String repeatable,
        /**
         * 变电站名称。
         */
        String stationName,
        /**
         * 平台生成的计划时间说明，可直接展示，不建议反向解析。
         */
        String planTimeContent) {
}
