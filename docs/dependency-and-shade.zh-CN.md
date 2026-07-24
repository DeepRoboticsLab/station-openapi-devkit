# 源码与依赖接入

**语言：** [English](dependency-and-shade.md) | 简体中文

Station OpenAPI SDK 通过公司 GitHub 公开仓库提供源码，不在 Maven Central 发布官方 Jar。当前源码和私有制品版本均为 `1.0.0-SNAPSHOT`。

客户可以直接复制源码，也可以把 SDK 发布到自己的私有 Nexus。

## 1. 直接复制源码

把下列源码和资源合并到 Spring Boot 模块的对应目录，并保留原 package：

| 仓库中的内容 | 客户项目中的目标目录 |
|---|---|
| `station-openapi-sdk-core/src/main/java/` 下的全部内容 | `src/main/java/` |
| `station-openapi-sdk-transport-okhttp/src/main/java/` 下的全部内容 | `src/main/java/` |
| `station-openapi-sdk-transport-okhttp/src/main/resources/` 下的全部内容 | `src/main/resources/` |

`src/main/resources` 中包含默认 HTTP transport 的 JDK SPI 注册文件，必须一起复制：

```text
META-INF/services/com.deeprobotics.station.openapi.sdk.http.HttpTransportProvider
```

如果漏掉该文件，`StationOpenApiClients.builder().build()` 将无法发现默认 OkHttp 实现。

使用 `spring-boot-starter-web` 的项目通常已经包含 Jackson 和 SLF4J。还需确认项目包含 OkHttp：

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

SDK 使用的准确依赖版本以仓库根 `pom.xml` 为准。复制源码后不要只挑选部分 `internal` 包，公共 API 的执行实现也位于这些包中。

### 需要 RocketMQ

如需使用 SDK 消息模型、Router 或原生 Subscriber，再复制：

```text
station-openapi-sdk-rocketmq/src/main/java/
```

Spring Boot 推荐参考 Sample 的 [主模块 POM](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/pom.xml)，同时使用 SDK RocketMQ 源码和 RocketMQ Starter。消息配置见[RocketMQ 消息接入](mq-events-guide.zh-CN.md)。

## 2. 发布到自己的私有 Nexus

如果多个项目需要复用 SDK，建议由客户团队自行构建并发布到企业 Nexus。

请使用 JDK 17 和 Maven 3.9+ 构建仓库根项目，并通过企业现有的 Maven/Nexus 发布配置发布完整 SDK 模块。不要只上传 `station-openapi-sdk` 这一个 Jar，因为它还依赖 core、OkHttp transport 及对应 POM 元数据。

完成发布后，Spring Boot 项目使用：

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

需要 RocketMQ 时再增加：

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk-rocketmq</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

业务项目还需要在自己的 Maven 配置中声明企业 Nexus 地址和认证信息。SDK 仓库不会提供或修改客户的 Nexus 配置。

## 3. 默认依赖和冲突处理

`station-openapi-sdk` 是普通 HTTP 接入使用的默认聚合包，包含：

- `station-openapi-sdk-core`：公共 API、请求响应模型、鉴权、异常和重试；
- `station-openapi-sdk-transport-okhttp`：默认 HTTP 实现；
- OkHttp 及其传递依赖。

如果项目已经使用其他版本的 OkHttp、Okio 或 Kotlin，先在 IDEA 的 Maven 依赖视图中确认最终版本并统一依赖。不要同时引入多个版本，也不要把 OkHttp 改成 `provided`。

只有常规版本对齐无法解决冲突时，才使用 `station-openapi-sdk-all`：

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk-all</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

`station-openapi-sdk-all` 与默认 `station-openapi-sdk` 二选一。它是依赖冲突的备用包，不是默认选择。

## 4. 接入后确认

- 应用使用 JDK 17 或更高版本。
- 源码接入时已经复制 SPI 资源文件。
- 私有 Nexus 中包含依赖所需的全部 SDK POM 和 Jar。
- `StationOpenApiClients.builder().build()` 能正常创建 Client。
- 首次调用 `client.system().getTimeText()` 成功。
