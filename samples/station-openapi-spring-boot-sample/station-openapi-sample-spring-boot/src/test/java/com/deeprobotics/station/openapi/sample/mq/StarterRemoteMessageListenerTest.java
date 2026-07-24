package com.deeprobotics.station.openapi.sample.mq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageConsumeResult;
import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageRouter;
import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageTags;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StarterRemoteMessageListenerTest {

    private RemoteMessageRouter router;
    private StarterRemoteMessageListener listener;

    @BeforeEach
    void setUp() {
        router = mock(RemoteMessageRouter.class);
        listener = new StarterRemoteMessageListener(router);
    }

    @Test
    void listenerUsesTheCommonStarterAnnotationContract() {
        RocketMQMessageListener annotation = StarterRemoteMessageListener.class
                .getAnnotation(RocketMQMessageListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.topic()).isEqualTo(RemoteMessageTags.TOPIC_HTTP_REMOTE);
        assertThat(annotation.consumerGroup()).isEqualTo("${station.openapi.mq.consumer-group}");
        assertThat(annotation.selectorExpression()).isEqualTo(SampleMqTags.SUBSCRIPTION);
        assertThat(RocketMQListener.class).isAssignableFrom(StarterRemoteMessageListener.class);
    }

    @Test
    void ackResultReturnsNormallyAndRoutesExactlyOnce() {
        MessageExt message = message(RemoteMessageTags.TAG_TASK_STATUS);
        when(router.route(message.getTags(), message.getBody()))
                .thenReturn(RemoteMessageConsumeResult.ACK);

        assertThatCode(() -> listener.onMessage(message)).doesNotThrowAnyException();

        verify(router).route(message.getTags(), message.getBody());
    }

    @Test
    void reconsumeResultBecomesStarterFailureWithoutRawBody() {
        MessageExt message = message(RemoteMessageTags.TAG_TASK_STATUS);
        when(router.route(message.getTags(), message.getBody()))
                .thenReturn(RemoteMessageConsumeResult.RECONSUME_LATER);

        assertThatThrownBy(() -> listener.onMessage(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(RemoteMessageTags.TAG_TASK_STATUS)
                .hasMessageNotContaining("{}");

        verify(router).route(message.getTags(), message.getBody());
    }

    private static MessageExt message(String tag) {
        MessageExt message = new MessageExt();
        message.setTags(tag);
        message.setBody("{}".getBytes(StandardCharsets.UTF_8));
        return message;
    }
}
