package com.deeprobotics.station.openapi.sample.nativemq;

import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 由 Spring 管理 SDK 原生 Subscriber 的启动和关闭生命周期。
 *
 * <p>构建 Bean 时只完成配置和 Handler 注册，不连接 Broker；Spring 初始化阶段调用
 * {@code start}，应用关闭时调用 {@code close} 释放线程与网络资源。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NativeMqProperties.class)
public class NativeMqConfiguration {

    /**
     * 构建订阅 SDK 支持的四类消息、并由 Spring 管理生命周期的原生订阅器。
     */
    @Bean(initMethod = "start", destroyMethod = "close")
    @ConditionalOnProperty(
            prefix = "station.openapi.native-mq",
            name = "enabled",
            havingValue = "true")
    public RemoteMessageSubscriber remoteMessageSubscriber(
            NativeMqProperties properties,
            NativeMqHandlers handlers) {
        return RemoteMessageSubscriber.builder()
                .nameServer(properties.getNameServer())
                .consumerGroup(properties.getConsumerGroup())
                .onTaskStatus(handlers::handleTaskStatus)
                .onTaskResult(handlers::handleTaskResult)
                .onDogAlarm(handlers::handleDogAlarm)
                .onRoute(handlers::handleRoute)
                .build();
    }
}
