package com.deeprobotics.station.openapi.sdk.rocketmq;

/**
 * SDK 路由后的消费结果。
 *
 * <p>该枚举刻意不直接暴露 RocketMQ 的 {@code ConsumeConcurrentlyStatus}，避免直接使用
 * {@link RemoteMessageRouter} 的调用方与 RocketMQ 原生 listener 类型耦合。{@link RemoteMessageSubscriber}
 * 会在内部把它转换为 RocketMQ 的 ack 或稍后重试。</p>
 */
public enum RemoteMessageConsumeResult {

    /**
     * 消息已处理或按路由规则跳过，可以确认消费。
     */
    ACK,

    /**
     * 消息处理失败，应让 RocketMQ 后续重新投递。
     */
    RECONSUME_LATER
}
