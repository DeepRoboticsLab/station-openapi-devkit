package com.deeprobotics.station.openapi.sample.config;

import com.deeprobotics.station.openapi.sample.mq.SampleRemoteMessageHandlers;
import com.deeprobotics.station.openapi.sdk.rocketmq.RemoteMessageRouter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * MQ 启用时先校验必要配置，再把四个示例 Handler 注册到 SDK Router。
 *
 * <p>配置缺失会在应用启动阶段快速失败；消息 JSON 解析和类型路由仍由 SDK 统一完成。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SampleMqProperties.class)
@ConditionalOnProperty(prefix = "station.openapi.mq", name = "enabled", havingValue = "true")
public class SampleMqConfiguration {

    /**
     * 校验 Starter 必需配置，并注册四类类型安全的远程消息处理器。
     */
    @Bean
    public RemoteMessageRouter remoteMessageRouter(
            SampleMqProperties properties,
            Environment environment,
            SampleRemoteMessageHandlers handlers) {
        requireText(properties.getConsumerGroup(), "station.openapi.mq.consumer-group");
        requireText(environment.getProperty("rocketmq.name-server"), "rocketmq.name-server");
        return RemoteMessageRouter.builder()
                .onTaskStatus(handlers::handleTaskStatus)
                .onTaskResult(handlers::handleTaskResult)
                .onDogAlarm(handlers::handleDogAlarm)
                .onRoute(handlers::handleRoute)
                .build();
    }

    private static String requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " is required when Station OpenAPI MQ is enabled");
        }
        return value.trim();
    }
}
