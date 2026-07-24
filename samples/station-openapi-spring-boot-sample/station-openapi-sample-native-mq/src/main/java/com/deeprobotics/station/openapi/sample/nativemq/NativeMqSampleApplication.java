package com.deeprobotics.station.openapi.sample.nativemq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SDK 原生 RocketMQ 消费方式的非 Web 启动入口。
 *
 * <p>这是 Starter 注解方案之外的独立补充示例，只用于少数需要直接管理 SDK Subscriber 的场景。</p>
 */
@SpringBootApplication
public class NativeMqSampleApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(NativeMqSampleApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }
}
