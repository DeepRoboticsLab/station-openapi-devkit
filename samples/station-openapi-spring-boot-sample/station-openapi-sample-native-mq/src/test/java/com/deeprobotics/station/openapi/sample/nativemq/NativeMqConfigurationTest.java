package com.deeprobotics.station.openapi.sample.nativemq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageException;
import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageSubscriber;
import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageTags;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.DogAlarmMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.RouteMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskResultMessage;
import com.deeprobotics.station.openapi.sdk.rocketmq.model.TaskStatusMessage;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

class NativeMqConfigurationTest {

    @Test
    void buildsNativeSubscriberWithoutStartingIt() {
        NativeMqProperties properties = validProperties();

        RemoteMessageSubscriber subscriber =
                new NativeMqConfiguration().remoteMessageSubscriber(properties, new NativeMqHandlers());

        assertThat(subscriber.topic()).isEqualTo(RemoteMessageTags.TOPIC_HTTP_REMOTE);
        assertThat(subscriber.tagExpression())
                .isEqualTo(RemoteMessageTags.subscriptionExpression())
                .doesNotContain(RemoteMessageTags.TAG_DEVICE_POS);
        subscriber.close();
    }

    @Test
    void lifecycleBeanStartsAndClosesTheSubscriber() throws NoSuchMethodException {
        Method factoryMethod = NativeMqConfiguration.class.getDeclaredMethod(
                "remoteMessageSubscriber", NativeMqProperties.class, NativeMqHandlers.class);
        Bean bean = factoryMethod.getAnnotation(Bean.class);

        assertThat(bean).isNotNull();
        assertThat(bean.initMethod()).isEqualTo("start");
        assertThat(bean.destroyMethod()).isEqualTo("close");
    }

    @Test
    void blankNameServerFailsBeforeStartingConsumer() {
        NativeMqProperties properties = validProperties();
        properties.setNameServer(" ");

        assertThatThrownBy(() ->
                new NativeMqConfiguration().remoteMessageSubscriber(properties, new NativeMqHandlers()))
                .isInstanceOf(RemoteMessageException.class)
                .hasMessageContaining("nameServer is required");
    }

    @Test
    void blankConsumerGroupFailsBeforeStartingConsumer() {
        NativeMqProperties properties = validProperties();
        properties.setConsumerGroup("");

        assertThatThrownBy(() ->
                new NativeMqConfiguration().remoteMessageSubscriber(properties, new NativeMqHandlers()))
                .isInstanceOf(RemoteMessageException.class)
                .hasMessageContaining("consumerGroup is required");
    }

    @Test
    void nativeHandlersAcceptAllFourSdkMessageModels() {
        NativeMqHandlers handlers = new NativeMqHandlers();

        assertThatCode(() -> {
            handlers.handleTaskStatus(new TaskStatusMessage(
                    "E-1", "T-1", "Patrol", "2", null, null, "50", null, null));
            handlers.handleTaskResult(new TaskResultMessage(
                    "dog-001", "Robot Dog", "Patrol", "T-1", "Point", "P-1",
                    null, null, null, null, List.of(), "E-1", null, "0.86", "2",
                    null, null, "50", null, null));
            handlers.handleDogAlarm(new DogAlarmMessage("dog-001", "Robot Dog", "A-1", null));
            handlers.handleRoute(new RouteMessage(
                    "dog-001", "Robot Dog", "E-1", List.of(), List.of("P-1")));
        }).doesNotThrowAnyException();
    }

    private static NativeMqProperties validProperties() {
        NativeMqProperties properties = new NativeMqProperties();
        properties.setEnabled(true);
        properties.setNameServer("127.0.0.1:9876");
        properties.setConsumerGroup("station-openapi-native-sample-test");
        return properties;
    }
}
