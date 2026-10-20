# Spring Boot 快速开始

**语言：** [English](quickstart.md) | 简体中文

本指南说明如何把 Station OpenAPI SDK 接入现有 Spring Boot 3 项目，并完成第一次平台调用。SDK 最低要求为 JDK 17。

## 1. 获取平台配置

请先联系项目对接的技术支持人员，获取：

- 以 `http://` 或 `https://` 开头的 Station OpenAPI 根地址；
- AppKey 和 SecretKey，或 Access Token；
- 凭证可访问的业务范围。

Endpoint 可以包含平台部署所需的路径，但不要追加具体的 `/remoteApi/*` 地址。模板启停需要平台 v1.3.3+；MQ
字段的版本要求见[RocketMQ 消息接入](mq-events-guide.zh-CN.md)。

## 2. 把 SDK 放入项目

当前 SDK 通过 GitHub 源码提供，不在 Maven Central 发布官方制品。请选择一种方式：

- **源码接入**：把 SDK core、OkHttp transport 及 SPI 资源复制到自己的项目。
- **Maven 依赖**：将 SDK 安装到本地仓库或发布到私有 Nexus，再依赖 `1.1.0`。

准备步骤见[源码与依赖接入](dependency-and-shade.zh-CN.md)。本地或私有 Maven 仓库中已有 SDK 后，业务项目添加：

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 3. 创建 Spring Boot 配置

将 Sample 的这两个类复制到 Spring Boot 能扫描到的包下，并调整 package：

- [StationOpenApiProperties.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiProperties.java)
- [StationOpenApiConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java)

它们负责配置绑定、启动校验，并通过 `destroyMethod="close"` 管理单例 `StationOpenApiClient`。所有调用复用该 Bean。

## 4. 配置一种鉴权方式

Sample 使用三个配置文件把公共配置与两种鉴权方式分开。自己的项目可以沿用同样结构。

### Token 模式

`application.yml`：

```yaml
spring:
  profiles:
    active: token

station:
  openapi:
    endpoint: ${STATION_ENDPOINT:}
    connect-timeout: 3s
    read-timeout: 10s
    call-timeout: 15s # 仅用于普通 JSON 调用
```

`application-token.yml`：

```yaml
spring:
  config:
    activate:
      on-profile: token

station:
  openapi:
    auth-mode: ACCESS_TOKEN
    access-token: ${STATION_ACCESS_TOKEN:}
```

### 签名模式

把 `application.yml` 中的 `spring.profiles.active` 改为 `signature`，并增加 `application-signature.yml`：

```yaml
spring:
  config:
    activate:
      on-profile: signature

station:
  openapi:
    auth-mode: SIGNATURE
    app-key: ${STATION_APP_KEY:}
    secret-key: ${STATION_SECRET_KEY:}
```

设置 `STATION_ENDPOINT` 和所选模式对应的凭证环境变量。`token` 与 `signature` 只启用一个，并只配置该模式的凭证。真实凭证不要提交到版本库。

更多说明见[鉴权与凭证](auth-and-sign.zh-CN.md)。

## 5. 完成第一次调用

在业务 Service 中注入单例 Client，先调用不会修改数据的平台时间接口：

```java
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import org.springframework.stereotype.Service;

@Service
public class StationConnectionService {

    private final StationOpenApiClient client;

    public StationConnectionService(StationOpenApiClient client) {
        this.client = client;
    }

    public String getPlatformTime() {
        return client.system().getTimeText();
    }
}
```

七个 API
分组的请求构造和调用方式见 [Sample Service 目录](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/)。

## 6. 处理调用结果

- 分页字段使用 `pageNo`，从 1 开始；不要使用 `pageNum`。
- 捕获 `StationOpenApiException` 处理 HTTP SDK 调用失败，可记录其 `requestId`、`operation`、`code` 和 `resultUnknown`。
- 不要记录完整请求体、响应体或凭证。
- 写入或控制操作失败且 `resultUnknown=true` 时，先查询状态或等待 MQ 消息，再决定是否重新执行。
- 文件下载使用 `OutputStream`，只尝试一次；失败时可能留下部分输出。

详细行为分别见[重试与结果确认](retry-and-idempotency.zh-CN.md)和[文件下载](file-download-guide.zh-CN.md)。

## 7. 运行完整 Sample

需要查看全部 28 个调用、请求参数和返回结构时，请在 IDEA
中运行 [Spring Boot Sample](../samples/station-openapi-spring-boot-sample/README.zh-CN.md)，然后通过 Swagger UI 调用。
