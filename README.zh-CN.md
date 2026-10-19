# Station OpenAPI Java SDK

**语言：** [English](README.md) | 简体中文

Station OpenAPI Java SDK 面向使用 JDK 17 和 Spring Boot 3 的应用，提供 28 个 HTTP 接口、两种鉴权方式、统一分页与异常处理、流式文件下载，以及可选的 RocketMQ 消息接入。

当前源码版本为 `1.0.0-SNAPSHOT`。项目通过公司 GitHub 公开仓库提供源码，不在 Maven Central 发布官方 Jar 或公共 Maven 坐标。

## 接入前准备

开始前请准备：

| 项目 | 要求 |
|---|---|
| JDK | 17 或更高版本 |
| Spring Boot | 3.x |
| Maven | 3.9 或更高版本 |
| 开发工具 | 推荐 IntelliJ IDEA |

请先联系项目对接的技术支持人员，获取当前环境的：

- Station OpenAPI 地址，例如 `http://station.example.com`；
- AppKey 和 SecretKey，或 Access Token，两种鉴权凭证任选其一；
- 如需接收 RocketMQ 消息，再获取 NameServer 地址并确认消息权限。

新增的任务模板启停接口需要平台 **v1.3.3 或更高版本**；MQ 任务结果的 `alarmLevel` 从平台 v1.3.2 起提供，旧消息缺失时为 `null`。请与技术支持确认目标环境支持所需功能。

Endpoint 使用技术支持提供的、以 `http://` 或 `https://` 开头的完整根地址，不要自行追加 `/remoteApi/*` 接口路径。

## 获取和集成 SDK

从公司 GitHub 公开仓库克隆或下载本项目后，可以选择：

1. **复制源码到自己的 Spring Boot 项目**：适合直接二次开发，也是最简单的接入方式。
2. **发布到自己的私有 Nexus**：先自行构建 SDK，再把完整 SDK 模块发布到企业 Nexus，业务项目通过 Maven 依赖使用。

本项目不会提供上传到 Maven Central 的官方 Jar。具体目录、依赖和私有 Nexus 使用方式见[源码与依赖接入](docs/dependency-and-shade.zh-CN.md)。

## 快速体验

仓库内提供了可直接运行的 [Spring Boot Sample](samples/station-openapi-spring-boot-sample/README.zh-CN.md)，包含：

- Swagger UI 中的全部 28 个 HTTP 调用；
- Token 和签名两种鉴权配置；
- 默认锁定的 11 个写入或控制操作；
- 文件流式下载；
- RocketMQ Starter 和 SDK 原生消费者示例。

> [!WARNING]
> **Sample 使用边界与责任声明**
>
> `samples/station-openapi-spring-boot-sample/` 仅用于展示 Station OpenAPI SDK 的配置、调用和消息接入方式，**不是可直接部署的生产系统**。Sample 未提供真实生产环境通常必需的身份认证、权限控制、数据持久化、审计、限流、监控告警、高可用、容灾及完整安全防护。
>
> **严禁在未完成必要的生产化设计、开发、安全评估和充分测试前，直接将 Sample 应用部署到真实生产环境或用于真实生产业务。任何因直接使用 Sample，或在未完成上述生产化工作的情况下将其投入生产，而造成的生产事故、设备误操作、服务中断、数据丢失、损坏或泄露、凭证泄露以及其他直接或间接损失，均由使用者自行承担；项目提供方和维护方不承担任何责任。**

第一次使用时，建议先按 [Sample README](samples/station-openapi-spring-boot-sample/README.zh-CN.md) 在 IDEA 中运行示例，再参考[快速开始](docs/quickstart.zh-CN.md)把 SDK 集成到自己的 Spring Boot 项目。

## 文档导航

| 需要了解的内容 | 文档 |
|---|---|
| 在 Spring Boot 中完成第一次调用 | [快速开始](docs/quickstart.zh-CN.md) |
| 复制源码或使用私有 Nexus | [源码与依赖接入](docs/dependency-and-shade.zh-CN.md) |
| 选择 Token 或签名鉴权 | [鉴权与凭证](docs/auth-and-sign.zh-CN.md) |
| 配置 Endpoint、超时和 Client | [HTTP 与超时配置](docs/http-client-and-timeout.zh-CN.md) |
| 处理重试和结果未知 | [重试与结果确认](docs/retry-and-idempotency.zh-CN.md) |
| 下载地图或巡检结果文件 | [文件下载](docs/file-download-guide.zh-CN.md) |
| 接收四类 RocketMQ 消息 | [RocketMQ 消息接入](docs/mq-events-guide.zh-CN.md) |
| 完成基础接入核对 | [接入检查](docs/release-checklist.zh-CN.md) |

## 使用时请注意

- 一个 `StationOpenApiClient` 只能配置一种鉴权方式，并应作为 Spring 单例 Bean 长期复用。
- `secretKey` 只参与本地签名，不会发送给平台；不要在日志、响应或代码仓库中保存完整凭证。
- 设备控制、任务下发和模板写入等操作可能改变现场状态，调用前请确认目标和当前状态。
- 写入或控制操作出现 `resultUnknown=true` 时，先查询状态或等待 MQ 消息，不要立即重复调用。
- 文件使用流式接口下载，不要把完整文件读入 `byte[]`。
- 摄像机密码、原始 HTTP 请求体和原始 MQ 消息体不应写入日志或返回给外部调用方。

版本变化见 [CHANGELOG.zh-CN.md](CHANGELOG.zh-CN.md)。
