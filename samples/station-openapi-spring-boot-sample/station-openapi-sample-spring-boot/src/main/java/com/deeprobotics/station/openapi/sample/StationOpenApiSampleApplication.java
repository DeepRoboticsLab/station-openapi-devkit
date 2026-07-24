package com.deeprobotics.station.openapi.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 主 HTTP Sample 的 Spring Boot 启动入口。
 *
 * <p>HTTP、Swagger 和 Starter MQ 共用同一应用，但平台调用始终通过 SDK 公共 API 完成。</p>
 */
@SpringBootApplication
public class StationOpenApiSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(StationOpenApiSampleApplication.class, args);
    }
}
