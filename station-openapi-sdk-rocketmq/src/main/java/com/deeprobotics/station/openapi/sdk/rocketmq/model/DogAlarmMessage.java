package com.deeprobotics.station.openapi.sdk.rocketmq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 四足狗本体告警推送消息。
 *
 * <p>对应 {@code uploadHttpRemoteAlarm} tag。告警编码按字符串透传，兼容平台以数字或字符串
 * 序列化的历史数据。告警时间取自平台记录，旧消息未提供时保留 null，不以消费时间补齐。</p>
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
        String alarmContent,
        /**
         * 告警发生时间，格式为 {@code yyyy-MM-dd HH:mm:ss}，不携带时区。
         * 与其他 MQ 时间字段一样保留文本；跨时区使用时应结合平台部署时区，旧消息缺失时为 null。
         */
        String alarmTime) {

    /**
     * 保留未包含告警时间时的四参构造器，兼容已有调用方。
     *
     * @param dogCode 四足狗编码
     * @param dogName 四足狗名称
     * @param alarmCode 告警编码
     * @param alarmContent 告警说明文本
     */
    public DogAlarmMessage(String dogCode, String dogName, String alarmCode, String alarmContent) {
        this(dogCode, dogName, alarmCode, alarmContent, null);
    }
}
