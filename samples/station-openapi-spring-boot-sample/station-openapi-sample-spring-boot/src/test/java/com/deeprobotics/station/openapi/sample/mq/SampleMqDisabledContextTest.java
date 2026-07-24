package com.deeprobotics.station.openapi.sample.mq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import com.deeprobotics.station.openapi.sample.config.SampleMqConfiguration;
import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageConsumeResult;
import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageRouter;
import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageTags;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;

@SpringBootTest(webEnvironment = NONE, properties = {
        "station.openapi.endpoint=http://127.0.0.1:9",
        "station.openapi.auth-mode=ACCESS_TOKEN",
        "station.openapi.access-token=sample-mq-disabled-token",
        "station.openapi.mq.enabled=false"
})
class SampleMqDisabledContextTest {

    private final ApplicationContextRunner configDataRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    private final ApplicationContextRunner mqRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    SampleMqConfiguration.class,
                    SampleRemoteMessageHandlers.class,
                    StarterRemoteMessageListener.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void disabledMqStartsWithoutListenerProducerOrBrokerConfiguration() {
        assertThat(applicationContext.getBeansOfType(StarterRemoteMessageListener.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(RemoteMessageRouter.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(DefaultRocketMQListenerContainer.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(RocketMQTemplate.class)).isEmpty();
        configDataRunner.withPropertyValues("logging.level.root=DEBUG")
                .run(context -> {
                    assertThat(context.getEnvironment().getProperty("logging.level.root"))
                            .isEqualTo("DEBUG");
                    assertThat(context.getEnvironment().getProperty(
                            "logging.level.org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer"))
                            .isEqualTo("INFO");
                });
    }

    @Test
    void enabledMqRequiresConsumerGroupBeforeCreatingListener() {
        mqRunner.withPropertyValues(
                        "station.openapi.mq.enabled=true",
                        "rocketmq.name-server=127.0.0.1:9876")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()).getMessage())
                            .contains("station.openapi.mq.consumer-group is required");
                });
    }

    @Test
    void enabledMqRequiresNameServerBeforeCreatingListener() {
        mqRunner.withPropertyValues(
                        "station.openapi.mq.enabled=true",
                        "station.openapi.mq.consumer-group=sample-consumer")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()).getMessage())
                            .contains("rocketmq.name-server is required");
                });
    }

    @Test
    void completeConfigurationCreatesOneListenerAndRoutesFourSdkTags() {
        mqRunner.withPropertyValues(
                        "station.openapi.mq.enabled=true",
                        "station.openapi.mq.consumer-group=sample-consumer",
                        "rocketmq.name-server=127.0.0.1:9876")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(StarterRemoteMessageListener.class);
                    assertThat(context).hasSingleBean(RemoteMessageRouter.class);
                    RemoteMessageRouter router = context.getBean(RemoteMessageRouter.class);
                    for (String tag : RemoteMessageTags.supportedTags()) {
                        assertThat(router.route(tag, "{}".getBytes(StandardCharsets.UTF_8)))
                                .isEqualTo(RemoteMessageConsumeResult.ACK);
                    }
                });
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
