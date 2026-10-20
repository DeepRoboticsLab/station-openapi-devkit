package com.deeprobotics.station.openapi.sdk.model.response.dog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * 四足狗本体告警记录。
 *
 * <p>表示 {@code /remoteApi/alarmPage} 返回的分页记录。
 * {@code alarmTime} 使用 SDK 统一的 {@code yyyy-MM-dd HH:mm:ss} 时间格式解析。</p>
 *
 * <p>该模型只表示狗端自身运行告警，例如电量、通信或底层设备状态相关告警。巡检点识别结果、
 * 图片资源和文件下载属于结果接口范围，后续不要把这些字段混入本模型。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DogAlarmInfo(
        /**
         * 四足狗名称。
         *
         * <p>用于告警列表展示。名称可能随台账维护变化，
         * 不建议作为业务唯一键。</p>
         */
        String dogName,
        /**
         * 四足狗编码。
         *
         * <p>用于把告警记录与台账、状态、坐标等接口结果关联。
         * 调用方进行去重、过滤或跨接口关联时应优先使用该字段。</p>
         */
        String dogCode,
        /**
         * 告警发生时间。
         *
         * <p>平台按 {@code yyyy-MM-dd HH:mm:ss} 输出，
         * SDK 的 JSON 层会解析为 {@link LocalDateTime}；该值不携带时区，调用方跨时区展示时应结合平台部署时区处理。</p>
         */
        LocalDateTime alarmTime,
        /**
         * 告警编码。
         *
         * <p>用于调用方匹配告警类型、配置过滤条件或做统计聚合。
         * SDK 不维护完整枚举表，以便兼容平台新增告警码。</p>
         */
        String alarmCode,
        /**
         * 告警内容。
         *
         * <p>这是平台生成的告警说明文本。调用方可按自身国际化
         * 与告警呈现策略直接使用，或结合 {@link #alarmCode()} 替换为自己的展示文案。</p>
         */
        String alarmContent) {
}
