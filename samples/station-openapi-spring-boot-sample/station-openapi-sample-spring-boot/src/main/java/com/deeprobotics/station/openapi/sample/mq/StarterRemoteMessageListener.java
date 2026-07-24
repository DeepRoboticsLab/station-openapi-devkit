package com.deeprobotics.station.openapi.sample.mq;

import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageConsumeResult;
import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageRouter;
import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageTags;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ Starter 最常见的注解式消费者示例。
 *
 * <p>监听器只负责把 tag 和原始字节交给 SDK Router；Router 已统一处理 JSON 兼容与消息模型转换。</p>
 * <p>业务处理成功后方法正常返回并由容器确认消费；需要重投时通过异常交还 Starter 处理。</p>
 */
@Component
@ConditionalOnProperty(prefix = "station.openapi.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = RemoteMessageTags.TOPIC_HTTP_REMOTE,
        consumerGroup = "${station.openapi.mq.consumer-group}",
        selectorExpression = SampleMqTags.SUBSCRIPTION)
public final class StarterRemoteMessageListener implements RocketMQListener<MessageExt> {

    private final RemoteMessageRouter router;

    public StarterRemoteMessageListener(RemoteMessageRouter router) {
        this.router = router;
    }

    @Override
    public void onMessage(MessageExt message) {
        RemoteMessageConsumeResult result = router.route(message.getTags(), message.getBody());
        // Router 请求稍后重试时抛出异常，让 Starter 按 RocketMQ 语义重投；正常返回则确认消费成功。
        if (result == RemoteMessageConsumeResult.RECONSUME_LATER) {
            throw new IllegalStateException(
                    "Remote message handler requested reconsume: tag=" + message.getTags());
        }
    }
}
