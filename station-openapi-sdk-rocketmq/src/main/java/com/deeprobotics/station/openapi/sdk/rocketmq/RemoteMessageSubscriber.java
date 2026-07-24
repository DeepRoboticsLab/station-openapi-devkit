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
 * 客户侧远程推送订阅入口。
 *
 * <p>该类使用 RocketMQ 原生 client 创建 consumer，但公共 builder 只暴露业务 handler，
 * 不要求客户实现 RocketMQ listener。handler 正常返回即 ack，抛异常则返回重投状态。</p>
 *
 * <p>同一 consumer group 下的多个实例使用 RocketMQ 集群消费语义，消息会在实例间负载均衡，
 * 并不是每个实例各收到一份。如果多个独立客户系统都需要完整消息流，应使用不同 consumer group。</p>
 *
 * <p>建议把 subscriber 注册为应用级单例，并在 Spring Bean 销毁回调或进程关闭阶段调用 {@link #close()}。
 * 构造对象不会连接 broker，只有 {@link #start()} 才创建并启动真实 consumer。关闭后允许重新启动，
 * 这是为了让容器生命周期具备容错能力，并不建议按消息或业务调用频繁重建 consumer。</p>
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
     * 启动 RocketMQ push consumer。
     *
     * <p>当前 consumer 已启动时重复调用会被忽略；{@link #close()} 后再次调用会创建新的 consumer。
     * 启动失败会包装为 SDK 自己的运行时异常，避免公共签名暴露 RocketMQ 受检异常类型。</p>
     *
     * <p>至少可以只注册客户关心的 handler；未注册类型仍在订阅表达式内，但收到后会 ack-skip。
     * 解析失败固定请求重投，不会静默丢弃消息。</p>
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
     * 关闭当前 consumer。
     *
     * <p>未启动或已关闭时重复调用不会产生副作用。关闭后对象回到未启动状态，后续可以再次调用
     * {@link #start()}；正常应用仍应由容器统一管理这个单例的启停。</p>
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
            /*
             * RocketMQ 可能按批次交付。只要任意一条返回 RECONSUME_LATER，整个批次都会被 broker 重投；
             * 因此前面已经成功执行的 handler 也可能再次被调用。SDK 不能在这里假装逐条 ack，客户 handler
             * 必须用 taskExecuteCode/devicePointCode 等业务键实现幂等 upsert。
             */
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
     * 原生 RocketMQ push consumer 的客户侧构造器。
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
         * @param consumerGroup 客户应用使用的消费组，不能为 null 或空白
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
