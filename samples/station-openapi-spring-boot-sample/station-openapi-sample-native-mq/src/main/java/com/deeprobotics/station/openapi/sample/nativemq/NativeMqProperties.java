package com.deeprobotics.station.openapi.sample.nativemq;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SDK 原生 RocketMQ 消费示例的独立配置。
 *
 * <p>原生示例单独绑定 NameServer 和消费者组；与 Starter 同时演示时必须使用不同消费者组，
 * 避免两个示例竞争同一组消息。</p>
 */
@ConfigurationProperties(prefix = "station.openapi.native-mq")
public class NativeMqProperties {

    private boolean enabled;
    private String nameServer;
    private String consumerGroup;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
}
