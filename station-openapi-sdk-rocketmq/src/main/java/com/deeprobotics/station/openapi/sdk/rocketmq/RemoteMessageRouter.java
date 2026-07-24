package com.deeprobotics.station.openapi.sdk.rocketmq;

import com.deeprobotics.station.openapi.sdk.exception.Redactor;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.DogAlarmMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.RouteMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskResultMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskStatusMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 独立于 RocketMQ listener 的远程推送消息路由器。
 *
 * <p>该类不依赖 RocketMQ 原生 listener，因此可以在单元测试中直接用 tag 和 body 验证解析规则。
 * {@link RemoteMessageSubscriber} 只负责把 RocketMQ 的 {@code MessageExt} 转成这里的输入。</p>
 */
public final class RemoteMessageRouter {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteMessageRouter.class);

    private final JsonCodec jsonCodec;
    private final RemoteMessageHandler<TaskStatusMessage> taskStatusHandler;
    private final RemoteMessageHandler<TaskResultMessage> taskResultHandler;
    private final RemoteMessageHandler<DogAlarmMessage> dogAlarmHandler;
    private final RemoteMessageHandler<RouteMessage> routeHandler;

    private RemoteMessageRouter(Builder builder) {
        this.jsonCodec = builder.jsonCodec;
        this.taskStatusHandler = builder.taskStatusHandler;
        this.taskResultHandler = builder.taskResultHandler;
        this.dogAlarmHandler = builder.dogAlarmHandler;
        this.routeHandler = builder.routeHandler;
    }

    /**
     * 创建路由器构造器。未注册的消息类型会被 ack-skip。
     *
     * @return 新的路由器构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 按 RocketMQ tag 解析并分派消息。
     *
     * <p>未知 tag、未注册 handler、以及坐标 tag 都会直接 ack-skip。当前 SDK 只路由
     * {@link RemoteMessageTags#supportedTags()} 返回的四类消息；平台新增其他 tag 时不会因缺少 handler 而反复重投。</p>
     *
     * <p>已支持 tag 的 JSON 解析失败或 handler 抛异常会返回 {@link RemoteMessageConsumeResult#RECONSUME_LATER}。
     * SDK 不提供“解析失败后静默 ack 丢弃”的开关。</p>
     *
     * @param tag RocketMQ 消息 tag
     * @param body UTF-8 消息体，可为 JSON object 或外层再包一层的 JSON string
     * @return ack 或稍后重投结果
     */
    public RemoteMessageConsumeResult route(String tag, byte[] body) {
        if (!RemoteMessageTags.isSupportedTag(tag)) {
            if (RemoteMessageTags.isDevicePositionTag(tag)) {
                LOG.debug("跳过坐标推送 tag，坐标请通过 /remoteApi/dogPos 拉取: tag={}", tag);
            } else {
                LOG.debug("跳过未支持的远程推送 tag: tag={}", tag);
            }
            return RemoteMessageConsumeResult.ACK;
        }

        if (RemoteMessageTags.TAG_TASK_STATUS.equals(tag)) {
            return dispatch(tag, body, TaskStatusMessage.class, taskStatusHandler);
        }
        if (RemoteMessageTags.TAG_TASK_RESULT.equals(tag)) {
            return dispatch(tag, body, TaskResultMessage.class, taskResultHandler);
        }
        if (RemoteMessageTags.TAG_DOG_ALARM.equals(tag)) {
            return dispatch(tag, body, DogAlarmMessage.class, dogAlarmHandler);
        }
        if (RemoteMessageTags.TAG_ROUTE.equals(tag)) {
            return dispatch(tag, body, RouteMessage.class, routeHandler);
        }
        return RemoteMessageConsumeResult.ACK;
    }

    private <T> RemoteMessageConsumeResult dispatch(String tag, byte[] body, Class<T> messageType,
            RemoteMessageHandler<T> handler) {
        if (handler == null) {
            LOG.debug("远程推送 tag 未注册 handler，消息已 ack-skip: tag={}", tag);
            return RemoteMessageConsumeResult.ACK;
        }
        try {
            T message = readMessage(body, messageType);
            handler.handle(message);
            return RemoteMessageConsumeResult.ACK;
        } catch (Exception ex) {
            /*
             * 解析失败或客户 handler 失败都不能 ack，否则客户无法通过 RocketMQ 重投恢复。
             * 日志只记录 tag、异常类型和脱敏后的 message，不打印原始 body 或 Jackson source 片段，
             * 避免后续消息体新增敏感字段时泄露。
             */
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warn("远程推送消息处理失败，等待 RocketMQ 重投: tag={}, exceptionType={}, message={}",
                    tag, ex.getClass().getName(), Redactor.redact(ex.getMessage()));
            return RemoteMessageConsumeResult.RECONSUME_LATER;
        }
    }

    private <T> T readMessage(byte[] body, Class<T> messageType) {
        if (body == null || body.length == 0) {
            throw new RemoteMessageException("RocketMQ message body is required");
        }
        String payload = new String(body, StandardCharsets.UTF_8).trim();
        if (payload.isEmpty()) {
            throw new RemoteMessageException("RocketMQ message body is blank");
        }

        /*
         * 平台消息体可能是普通 JSON object，也可能是外层再包一层的 JSON string。
         * 因此这里统一先解开 string-wrapped JSON，再按目标模型解析。
         */
        if (payload.startsWith("\"")) {
            String unwrapped = jsonCodec.read(payload, String.class);
            payload = unwrapped == null ? "" : unwrapped.trim();
        }
        T message = jsonCodec.read(payload, messageType);
        if (message == null) {
            throw new RemoteMessageException("RocketMQ message JSON root must not be null");
        }
        return message;
    }

    /**
     * 路由器构造器。每种消息最多注册一个 handler。
     */
    public static final class Builder {

        private JsonCodec jsonCodec = JsonCodec.defaultCodec();
        private RemoteMessageHandler<TaskStatusMessage> taskStatusHandler;
        private RemoteMessageHandler<TaskResultMessage> taskResultHandler;
        private RemoteMessageHandler<DogAlarmMessage> dogAlarmHandler;
        private RemoteMessageHandler<RouteMessage> routeHandler;

        private Builder() {
        }

        /**
         * 注入消息解析 codec，主要用于测试或客户已有兼容 Jackson 配置的高级场景。
         *
         * @param jsonCodec 非 null 的 JSON 编解码器
         * @return 当前构造器
         */
        public Builder jsonCodec(JsonCodec jsonCodec) {
            if (jsonCodec == null) {
                throw new RemoteMessageException("jsonCodec is required");
            }
            this.jsonCodec = jsonCodec;
            return this;
        }

        /**
         * 注册任务状态 handler；传 {@code null} 等同于不注册。
         *
         * @param handler 任务状态处理器，允许为 {@code null}
         * @return 当前构造器
         */
        public Builder onTaskStatus(RemoteMessageHandler<TaskStatusMessage> handler) {
            this.taskStatusHandler = handler;
            return this;
        }

        /**
         * 注册巡检结果 handler；客户实现必须按业务键幂等。
         *
         * @param handler 巡检结果处理器，允许为 {@code null}
         * @return 当前构造器
         */
        public Builder onTaskResult(RemoteMessageHandler<TaskResultMessage> handler) {
            this.taskResultHandler = handler;
            return this;
        }

        /**
         * 注册四足狗本体告警 handler。
         *
         * @param handler 本体告警处理器，允许为 {@code null}
         * @return 当前构造器
         */
        public Builder onDogAlarm(RemoteMessageHandler<DogAlarmMessage> handler) {
            this.dogAlarmHandler = handler;
            return this;
        }

        /**
         * 注册巡视路线 handler。
         *
         * @param handler 巡视路线处理器，允许为 {@code null}
         * @return 当前构造器
         */
        public Builder onRoute(RemoteMessageHandler<RouteMessage> handler) {
            this.routeHandler = handler;
            return this;
        }

        /**
         * 构建不依赖真实 broker 的路由器，可直接用于单元测试或自定义消费适配层。
         *
         * @return 使用当前 handler 配置的路由器
         */
        public RemoteMessageRouter build() {
            return new RemoteMessageRouter(this);
        }
    }
}
