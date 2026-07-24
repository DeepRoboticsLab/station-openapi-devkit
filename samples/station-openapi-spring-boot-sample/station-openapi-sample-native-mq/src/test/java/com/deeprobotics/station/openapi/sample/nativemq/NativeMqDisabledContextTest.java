package com.deeprobotics.station.openapi.sample.nativemq;

import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageSubscriber;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NativeMqDisabledContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(NativeMqConfiguration.class, NativeMqHandlers.class);

    @Test
    void disabledNonWebContextStartsWithoutSubscriberOrRocketMqThreads() {
        Set<Long> existingThreadIds = threadIds();

        runner.withPropertyValues("station.openapi.native-mq.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(RemoteMessageSubscriber.class);
                    assertThat(context.getEnvironment().getProperty("spring.main.web-application-type"))
                            .isEqualTo("none");
                    assertThat(newRocketMqThreadNames(existingThreadIds)).isEmpty();
                });
    }

    private static Set<Long> threadIds() {
        return Thread.getAllStackTraces().keySet().stream()
                .map(Thread::getId)
                .collect(Collectors.toSet());
    }

    private static Set<String> newRocketMqThreadNames(Set<Long> existingThreadIds) {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .filter(thread -> !existingThreadIds.contains(thread.getId()))
                .map(Thread::getName)
                .filter(name -> name.toLowerCase().contains("rocketmq"))
                .collect(Collectors.toSet());
    }
}
