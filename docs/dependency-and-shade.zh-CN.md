# 源码与依赖接入

**语言：** [English](dependency-and-shade.md) | 简体中文

Station OpenAPI SDK 通过 GitHub 提供源码，不在 Maven Central 发布官方 Jar。当前源码版本为 `1.1.0`，要求 JDK 17
或更高版本。

可以直接复制源码、安装到本地 Maven 仓库，或发布到私有 Nexus。

## 1. 直接复制源码

把下列源码和资源合并到 Spring Boot 模块的对应目录，并保留原 package：

| 仓库中的内容                                                            | 客户项目中的目标目录  |
|-------------------------------------------------------------------------|-----------------------|
| `station-openapi-sdk-core/src/main/java/` 下的全部内容                  | `src/main/java/`      |
| `station-openapi-sdk-transport-okhttp/src/main/java/` 下的全部内容      | `src/main/java/`      |
| `station-openapi-sdk-transport-okhttp/src/main/resources/` 下的全部内容 | `src/main/resources/` |

`src/main/resources` 中包含默认 HTTP transport 的 JDK SPI 注册文件，必须一起复制：

```text
META-INF/services/com.deeprobotics.station.openapi.sdk.http.HttpTransportProvider
```

缺少该文件时，SDK 无法发现默认 OkHttp transport。

使用 `spring-boot-starter-web` 的项目通常已经包含 Jackson 和 SLF4J。还需确认项目包含 OkHttp：

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

依赖版本以[根 POM](../pom.xml) 为准。源码目录须完整复制，包括 `internal` 包。

### 需要 RocketMQ

如需使用 SDK 消息模型、Router 或原生 Subscriber，再复制：

```text
station-openapi-sdk-rocketmq/src/main/java/
```

同时引入 [RocketMQ 模块 POM](../station-openapi-sdk-rocketmq/pom.xml) 声明的原生 client 依赖。RocketMQ Starter
的依赖和配置见[RocketMQ 消息接入](mq-events-guide.zh-CN.md)。

## 2. 通过 Maven 仓库使用

本地开发时，使用 JDK 17 和 Maven 3.9+，在仓库根目录执行：

```shell
mvn clean install
```

多人共用时，通过企业 Maven/Nexus 配置发布父 POM 和 SDK 模块。仅上传 `station-openapi-sdk` Jar 不够，它还依赖 core、OkHttp
transport 及对应 POM。

安装或发布完成后，业务项目添加：

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk</artifactId>
    <version>1.1.0</version>
</dependency>
```

需要 RocketMQ 时再增加：

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk-rocketmq</artifactId>
    <version>1.1.0</version>
</dependency>
```

使用 Nexus 时，在业务项目的 Maven 环境中配置仓库地址和认证信息。

## 3. 默认依赖和冲突处理

`station-openapi-sdk` 是普通 HTTP 接入使用的默认聚合包，包含：

- `station-openapi-sdk-core`：公共 API、请求响应模型、鉴权、异常和重试；
- `station-openapi-sdk-transport-okhttp`：默认 HTTP 实现；
- OkHttp 及其传递依赖。

如果项目已经使用其他版本的 OkHttp、Okio 或 Kotlin，先在 IDEA 的 Maven 依赖视图中确认最终版本并统一依赖。不要同时引入多个版本，也不要把
OkHttp 改成 `provided`。

只有常规版本对齐无法解决冲突时，才使用 `station-openapi-sdk-all`：

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk-all</artifactId>
    <version>1.1.0</version>
</dependency>
```

`station-openapi-sdk-all` 与 `station-openapi-sdk` 二选一。该包包含 HTTP SDK，并重定位 `okhttp3`、`okio`、`kotlin`，不包含
RocketMQ；Jackson 和 SLF4J 保留原包名。

接下来按 [Spring Boot 快速开始](quickstart.zh-CN.md)配置 Client 并完成第一次调用。
