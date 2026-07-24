package com.deeprobotics.station.openapi.sample.mq;

import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageTags;

/**
 * RocketMQ 注解使用的编译期订阅表达式，所有 tag 均直接复用 SDK 常量。
 *
 * <p>注解属性必须是编译期常量，因此在此显式组合 SDK 支持的四个 Tag；
 * 设备位置消息不属于当前 SDK 支持的订阅范围。</p>
 */
public final class SampleMqTags {

    public static final String SUBSCRIPTION =
            RemoteMessageTags.TAG_TASK_STATUS + "||"
                    + RemoteMessageTags.TAG_TASK_RESULT + "||"
                    + RemoteMessageTags.TAG_DOG_ALARM + "||"
                    + RemoteMessageTags.TAG_ROUTE;

    private SampleMqTags() {
    }
}
