# Spring Boot 快速开始

**语言：** [English](quickstart.md) | 简体中文

本指南说明如何把 Station OpenAPI SDK 接入现有 Spring Boot 3 项目，并完成第一次平台调用。SDK 最低要求为 JDK 17。

## 1. 获取平台配置

请先联系项目对接的技术支持人员，获取：

- 以 `http://` 或 `https://` 开头的 Station OpenAPI 根地址；
- AppKey 和 SecretKey，或 Access Token；
- 凭证可访问的业务范围。

技术支持提供的平台环境已与 SDK 配套，无需自行选择平台版本。Endpoint 可以包含平台部署所需的路径，但不要追加具体的 `/remoteApi/*` 地址。

## 2. 把 SDK 放入项目

当前 SDK 通过 GitHub 源码提供，不在 Maven Central 发布官方制品。请选择一种方式：

- **源码接入**：把 SDK core、OkHttp transport 及 SPI 资源复制到自己的项目。
- **私有 Nexus**：自行构建并发布完整 SDK 模块，然后依赖 `1.0.0-SNAPSHOT`。

准确的目录和依赖见[源码与依赖接入](dependency-and-shade.zh-CN.md)。如果已经把 SDK 发布到自己的 Nexus，业务项目只需添加：

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

该依赖只有在你的私有 Nexus 已包含完整 SDK 模块时才能解析。

## 3. 创建 Spring Boot 配置

Sample 已提供完整的配置绑定和单例 Client Bean：

- [StationOpenApiProperties.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiProperties.java)
- [StationOpenApiConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java)

这两个类包含完整注释、启动参数校验和 Client 关闭配置。将它们复制到自己的 Spring Boot 项目后，可以按项目包名调整 package。

`StationOpenApiClient` 应保持为单例 Bean。不要在 Controller、Service 或每次业务调用中重复创建 Client。

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
    endpoint: http://station.example.com
    connect-timeout: 3s
    read-timeout: 10s
    call-timeout: 15s
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
    access-token: "<技术支持提供的 Access Token>"
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
    app-key: "<技术支持提供的 AppKey>"
    secret-key: "<技术支持提供的 SecretKey>"
```

两种模式不能同时启用。Token 模式不要再配置 AppKey/SecretKey；签名模式不要再配置 Access Token。真实凭证只应保存在本地配置或项目使用的凭证管理系统中，不要提交到公开仓库。

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

调用成功后，再根据业务需要参考 Sample 中七个 Service：

- [InventorySampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/InventorySampleService.java)
- [DogSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/DogSampleService.java)
- [CameraSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/CameraSampleService.java)
- [TaskTemplateSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/TaskTemplateSampleService.java)
- [TaskSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/TaskSampleService.java)
- [ResultSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/ResultSampleService.java)
- [SystemSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/SystemSampleService.java)

这些类直接构建 SDK 请求并调用公共 API，没有额外的后台依赖。

## 6. 处理调用结果

- 分页字段使用 `pageNo`，从 1 开始；不要使用 `pageNum`。
- 所有 SDK 异常都继承 `StationOpenApiException`，可记录其 `requestId`、`operation`、`code` 和 `resultUnknown`。
- 不要记录完整请求体、响应体或凭证。
- 写入或控制操作失败且 `resultUnknown=true` 时，先查询状态或等待 MQ 消息，再决定是否重新执行。
- 文件下载使用 `Path` 或 `OutputStream` 接口。

详细行为分别见[重试与结果确认](retry-and-idempotency.zh-CN.md)和[文件下载](file-download-guide.zh-CN.md)。

## 7. 运行完整 Sample

需要查看全部 27 个调用、请求参数和返回结构时，请在 IDEA 中运行 [Spring Boot Sample](../samples/station-openapi-spring-boot-sample/README.zh-CN.md)，然后通过 Swagger UI 调用。
