package com.deeprobotics.station.openapi.sample.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 主 Sample 的 RocketMQ Starter 消费配置。
 *
 * <p>这里只绑定示例开关和消费者组；NameServer 继续使用 Starter 通用的
 * {@code rocketmq.name-server} 配置，避免维护两套来源。</p>
 */
@ConfigurationProperties(prefix = "station.openapi.mq")
public class SampleMqProperties {

    private boolean enabled;
    private String consumerGroup;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
}
