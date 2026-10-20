package com.deeprobotics.station.openapi.sdk.rocketmq;

import com.deeprobotics.station.openapi.sdk.rocketmq.model.DogAlarmMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.RouteMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskResultMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskStatusMessage;
import java.util.List;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * 远程推送订阅入口。
 *
 * <p>使用 RocketMQ 原生客户端订阅消息，调用方只需注册 {@link RemoteMessageHandler}。</p>
 *
 * <p>同一消费组的实例分担消息；需要独立接收完整消息流的系统应使用不同消费组。</p>
 *
 * <p>建议作为应用级单例管理，在应用退出时调用 {@link #close()}。
 * 构造时不连接 broker，{@link #start()} 才启动消费，关闭后可重新启动。</p>
 */
public final class RemoteMessageSubscriber implements AutoCloseable {

    private final String nameServer;
    private final String consumerGroup;
    private final RemoteMessageRouter router;

    private DefaultMQPushConsumer consumer;

    private RemoteMessageSubscriber(Builder builder) {
        this.nameServer = builder.nameServer;
        this.consumerGroup = builder.consumerGroup;
        this.router = builder.routerBuilder.build();
    }

    /**
     * 创建订阅器构造器。
     *
     * @return 新的订阅器构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 启动 RocketMQ 推送消费者。
     *
     * <p>已启动时忽略重复调用；{@link #close()} 后可重新启动。
     * 启动失败抛出 {@link RemoteMessageException}。</p>
     *
     * <p>未注册处理器的消息确认并跳过；已注册类型解析或处理失败时请求重投。</p>
     */
    public synchronized void start() {
        if (consumer != null) {
            return;
        }

        DefaultMQPushConsumer created = new DefaultMQPushConsumer(consumerGroup);
        try {
            created.setNamesrvAddr(nameServer);
            created.subscribe(topic(), tagExpression());
            created.registerMessageListener(new RemoteMessageListener(router));
            created.start();
            consumer = created;
        } catch (MQClientException | RuntimeException ex) {
            created.shutdown();
            throw new RemoteMessageException("Failed to start Station OpenAPI RocketMQ subscriber", ex);
        }
    }

    /**
     * 当前 SDK 固定订阅的 topic。
     *
     * @return {@code httpRemote}
     */
    public String topic() {
        return RemoteMessageTags.TOPIC_HTTP_REMOTE;
    }

    /**
     * 当前 SDK 固定订阅的四个有效 tag 表达式。
     *
     * @return RocketMQ 原生订阅表达式
     */
    public String tagExpression() {
        return RemoteMessageTags.subscriptionExpression();
    }

    /**
     * 关闭当前消费者。
     *
     * <p>未启动或已关闭时无操作；关闭后可再次调用 {@link #start()}。</p>
     */
    @Override
    public synchronized void close() {
        if (consumer != null) {
            consumer.shutdown();
            consumer = null;
        }
    }

    private static final class RemoteMessageListener implements MessageListenerConcurrently {

        private final RemoteMessageRouter router;

        private RemoteMessageListener(RemoteMessageRouter router) {
            this.router = router;
        }

        @Override
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messages, ConsumeConcurrentlyContext context) {
            if (messages == null || messages.isEmpty()) {
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
            // 任一消息失败会重投整批，已成功的处理器也可能再次执行，必须按业务键幂等处理。
            for (MessageExt message : messages) {
                RemoteMessageConsumeResult result = router.route(message.getTags(), message.getBody());
                if (result == RemoteMessageConsumeResult.RECONSUME_LATER) {
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }

    /**
     * 原生 RocketMQ push consumer 的构造器。
     */
    public static final class Builder {

        private String nameServer;
        private String consumerGroup;
        private final RemoteMessageRouter.Builder routerBuilder = RemoteMessageRouter.builder();

        private Builder() {
        }

        /**
         * 设置 RocketMQ NameServer 地址，格式与原生 client 一致；多个地址使用分号分隔。
         *
         * @param nameServer NameServer 地址，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder nameServer(String nameServer) {
            this.nameServer = nameServer;
            return this;
        }

        /**
         * 设置 consumer group。
         *
         * <p>同组实例共同消费一份消息；需要独立接收完整消息流的系统必须使用不同 group。</p>
         *
         * @param consumerGroup 应用使用的消费组，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder consumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
            return this;
        }

        /**
         * 注册任务状态与进度 handler。
         *
         * @param handler 任务状态处理器，允许为 {@code null}
         * @return 当前构造器
         */
        public Builder onTaskStatus(RemoteMessageHandler<TaskStatusMessage> handler) {
            routerBuilder.onTaskStatus(handler);
            return this;
        }

        /**
         * 注册巡检结果 handler。同一点位可能重复推送，且批次重投也会再次调用，handler 必须幂等。
         *
         * @param handler 巡检结果处理器，允许为 {@code null}
         * @return 当前构造器
         */
        public Builder onTaskResult(RemoteMessageHandler<TaskResultMessage> handler) {
            routerBuilder.onTaskResult(handler);
            return this;
        }

        /**
         * 注册四足狗本体告警 handler。
         *
         * @param handler 本体告警处理器，允许为 {@code null}
         * @return 当前构造器
         */
        public Builder onDogAlarm(RemoteMessageHandler<DogAlarmMessage> handler) {
            routerBuilder.onDogAlarm(handler);
            return this;
        }

        /**
         * 注册巡视路线 handler。
         *
         * @param handler 巡视路线处理器，允许为 {@code null}
         * @return 当前构造器
         */
        public Builder onRoute(RemoteMessageHandler<RouteMessage> handler) {
            routerBuilder.onRoute(handler);
            return this;
        }

        /**
         * 校验 NameServer 和 consumer group 后构建订阅器；此时尚未连接 broker。
         *
         * @return 尚未启动的订阅器
         */
        public RemoteMessageSubscriber build() {
            this.nameServer = requireText(nameServer, "nameServer");
            this.consumerGroup = requireText(consumerGroup, "consumerGroup");
            return new RemoteMessageSubscriber(this);
        }

        private static String requireText(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new RemoteMessageException(name + " is required");
            }
            return value.trim();
        }
    }
}
