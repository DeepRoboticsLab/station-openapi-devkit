package com.deeprobotics.station.openapi.sdk.rocketmq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 任务状态/进度推送消息。
 *
 * <p>对应 {@code uploadHttpRemoteTaskStatus} tag。状态、进度和时间字段按平台字符串透传，
 * 便于调用方直接展示，也兼容平台数据中的数字或字符串编码。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskStatusMessage(
        /** 一次任务执行的唯一编码，是任务控制、结果查询和 MQ 事件关联的核心业务键。 */
        String taskExecuteCode,
        /** 任务模板编码，同一模板多次执行时该值相同。 */
        String taskCode,
        /** 本次执行记录中的任务名称。 */
        String taskName,
        /** 任务执行状态编码：1 已执行、2 正在执行、3 暂停、4 终止、5 未执行、6 超期。 */
        String status,
        /** 计划开始时间，平台格式为 {@code yyyy-MM-dd HH:mm:ss}。 */
        String planStartTime,
        /** 实际开始时间，任务未开始时可能为空。 */
        String startTime,
        /** 任务进度展示值，平台可能返回百分比或小数字符串，SDK 不做数值转换。 */
        String taskProgress,
        /** 预计剩余时间，平台按字符串透传。 */
        String remainTime,
        /** 平台返回的任务描述或执行说明。 */
        String description) {
}
