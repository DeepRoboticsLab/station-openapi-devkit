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
 * 按 tag 解析并分派远程推送消息，不依赖 RocketMQ 监听器类型。
 *
 * <p>可由 {@link RemoteMessageSubscriber} 或自定义消费适配层调用。</p>
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
     * 创建路由器构造器。未注册处理器的消息会被确认并跳过。
     *
     * @return 新的路由器构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 按 RocketMQ tag 解析并分派消息。
     *
     * <p>仅处理 {@link RemoteMessageTags#supportedTags()} 中的四类消息。
     * 未知 tag、坐标 tag 或未注册处理器的消息均确认并跳过。</p>
     *
     * <p>已注册处理器的消息解析失败，或处理器抛出异常时，返回
     * {@link RemoteMessageConsumeResult#RECONSUME_LATER} 请求重投。</p>
     *
     * @param tag RocketMQ 消息 tag
     * @param body UTF-8 消息体，可为 JSON 对象或再次编码为 JSON 字符串的对象
     * @return 确认或稍后重投
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
            // 失败时保留重投机会；日志仅记录 tag、异常类型和脱敏消息，不记录原始消息体。
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

        // 兼容平台将 JSON 对象再次编码为 JSON 字符串的格式。
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
         * 设置消息解析器，默认使用 {@link JsonCodec#defaultCodec()}。
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
         * 注册巡检结果 handler；处理器必须按业务键实现幂等。
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
         * 构建路由器，不连接 broker。
         *
         * @return 使用当前 handler 配置的路由器
         */
        public RemoteMessageRouter build() {
            return new RemoteMessageRouter(this);
        }
    }
}
