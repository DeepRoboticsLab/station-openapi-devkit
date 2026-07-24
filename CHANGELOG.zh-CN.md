# 变更日志

**语言：** [English](CHANGELOG.md) | 简体中文

## 1.0.0-SNAPSHOT

当前版本是 GitHub 源码版本，尚未作为正式版本发布，也不提供 Maven Central 公共制品。

### SDK

- 提供七个 API 分组，共 27 个 Station OpenAPI HTTP 调用。
- 支持 Access Token 和 AppKey/SecretKey 签名两种鉴权方式。
- 提供统一分页、异常、请求 ID、超时和受控重试。
- 支持通过 `Path` 或 `OutputStream` 流式下载文件。
- 提供可选的 RocketMQ Router、消息模型和原生 Subscriber。
- RocketMQ 支持任务状态、巡检结果、本体告警和巡视路线四类消息。
- 提供普通 OkHttp 依赖路径和依赖冲突备用包。

### Spring Boot Sample

- 使用 Spring Boot 3.5.16 和 JDK 17。
- Swagger UI 展示全部 27 个 SDK HTTP 调用。
- 支持 Token 和签名两种配置。
- 10 个写入或控制操作默认锁定。
- 提供中英文参数错误和 SDK 异常响应。
- 文件通过 `StreamingResponseBody` 输出。
- 提供 RocketMQ Starter 主示例和原生 Subscriber 补充示例。

### 获取方式

本项目通过公司 GitHub 公开仓库提供源码。客户可以：

- 把 SDK 源码复制到自己的 Spring Boot 项目；
- 自行构建完整 SDK 模块并发布到企业私有 Nexus。

接入方式见[源码与依赖接入](docs/dependency-and-shade.zh-CN.md)，运行示例见 [Sample README](samples/station-openapi-spring-boot-sample/README.zh-CN.md)。
