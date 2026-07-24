package com.deeprobotics.station.openapi.sdk.rocketmq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 四足狗本体告警推送消息。
 *
 * <p>对应 {@code uploadHttpRemoteAlarm} tag。告警编码按字符串透传，兼容平台以数字或字符串
 * 序列化的历史数据。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DogAlarmMessage(
        /** 四足狗编码。 */
        String dogCode,
        /** 四足狗名称。 */
        String dogName,
        /** 告警编码。 */
        String alarmCode,
        /** 平台生成的告警说明文本；是否直接展示由调用方的国际化和告警呈现策略决定。 */
        String alarmContent) {
}
