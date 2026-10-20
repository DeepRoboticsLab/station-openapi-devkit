package com.deeprobotics.station.openapi.sdk.rocketmq;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@code httpRemote} RocketMQ 推送 topic/tag 常量。
 *
 * <p>SDK 只订阅并路由任务状态、巡检结果、本体告警和巡视路线四个 tag。
 * {@link #TAG_DEVICE_POS} 仅作为识别坐标消息的兼容常量；实时坐标应通过 HTTP
 * {@code /remoteApi/dogPos} 查询。</p>
 */
public final class RemoteMessageTags {

    /**
     * 平台远程推送使用的固定 topic。
     */
    public static final String TOPIC_HTTP_REMOTE = "httpRemote";

    /**
     * 任务状态和进度推送 tag。
     */
    public static final String TAG_TASK_STATUS = "uploadHttpRemoteTaskStatus";

    /**
     * 巡检结果推送 tag。同一个点位结果可能先后推送原图、原图+效果图，调用方应按幂等键合并。
     */
    public static final String TAG_TASK_RESULT = "uploadHttpRemoteResult";

    /**
     * 四足狗本体告警推送 tag。
     */
    public static final String TAG_DOG_ALARM = "uploadHttpRemoteAlarm";

    /**
     * 巡视路线推送 tag。
     */
    public static final String TAG_ROUTE = "uploadHttpRemoteRoute";

    /**
     * 坐标消息 tag 兼容常量。SDK 不订阅、不路由该 tag。
     */
    public static final String TAG_DEVICE_POS = "uploadHttpRemoteDevicePos";

    private static final Set<String> SUPPORTED_TAGS = Set.of(
            TAG_TASK_STATUS,
            TAG_TASK_RESULT,
            TAG_DOG_ALARM,
            TAG_ROUTE);

    private static final String SUBSCRIPTION_EXPRESSION = String.join("||",
            TAG_TASK_STATUS,
            TAG_TASK_RESULT,
            TAG_DOG_ALARM,
            TAG_ROUTE);

    private RemoteMessageTags() {
    }

    /**
     * 返回当前 SDK 正常路由的四个 tag。
     *
     * @return 按稳定顺序排列的不可变 tag 集合
     */
    public static Set<String> supportedTags() {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.add(TAG_TASK_STATUS);
        ordered.add(TAG_TASK_RESULT);
        ordered.add(TAG_DOG_ALARM);
        ordered.add(TAG_ROUTE);
        return Collections.unmodifiableSet(ordered);
    }

    /**
     * RocketMQ 原生 client 使用的 tag 订阅表达式。
     *
     * @return 四个有效 tag 用 {@code ||} 连接的表达式
     */
    public static String subscriptionExpression() {
        return SUBSCRIPTION_EXPRESSION;
    }

    /**
     * 判断 tag 是否属于当前 SDK 支持的四类消息。
     *
     * @param tag RocketMQ 消息 tag，允许为 {@code null}
     * @return 属于四个有效 tag 时返回 {@code true}
     */
    public static boolean isSupportedTag(String tag) {
        return tag != null && SUPPORTED_TAGS.contains(tag);
    }

    /**
     * 判断 tag 是否是仅保留兼容常量、但不订阅路由的坐标 tag。
     *
     * @param tag RocketMQ 消息 tag，允许为 {@code null}
     * @return 是坐标 tag 时返回 {@code true}
     */
    public static boolean isDevicePositionTag(String tag) {
        return TAG_DEVICE_POS.equals(tag);
    }
}
