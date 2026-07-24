package com.deeprobotics.station.openapi.sample.mq;

import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageTags;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SampleMqTagsTest {

    @Test
    void selectorMatchesTheSdkContractExactly() {
        assertThat(SampleMqTags.SUBSCRIPTION)
                .isEqualTo(RemoteMessageTags.subscriptionExpression())
                .doesNotContain(RemoteMessageTags.TAG_DEVICE_POS);
        assertThat(Set.of(SampleMqTags.SUBSCRIPTION.split("\\|\\|")))
                .containsExactlyInAnyOrderElementsOf(RemoteMessageTags.supportedTags());
    }
}
