# 变更日志

**语言：** [English](CHANGELOG.md) | 简体中文

## 1.0.0-SNAPSHOT

当前版本是 GitHub 源码版本，尚未作为正式版本发布，也不提供 Maven Central 公共制品。

### 破坏性变更：全程流式下载

- 删除 `ResultApi.downloadResource(request, Path[, options])` 的两个重载，仅保留 `OutputStream` 重载；原调用方需要迁移并重新编译。
- HTTP 数据通过 8 KiB 缓冲区直接写入调用方输出流。SDK 不再创建临时文件、完整缓存文件，也不再管理本地目录、替换和清理。
- 下载始终只尝试一次，包括设置 `RequestOptions.RetryPolicy.DEFAULT` 时。连接、超时或输出失败可能留下部分数据，异常标记 `resultUnknown=true`；重新下载前应丢弃不完整输出。普通查询保留原有重试行为。
- 输出流由调用方持有并关闭。保存文件时，将原 `Path` 调用改为：

```java
try (OutputStream output = Files.newOutputStream(target)) {
    client.result().downloadResource(request, output, options);
}
```

`Files.newOutputStream` 会立即创建或清空目标文件，且要求父目录已存在。文件权限、覆盖策略和不完整文件清理由调用方负责。Spring Boot 的 `StreamingResponseBody` 随数据到达直接转发；HTTP 响应提交后发生的失败无法再转换为完整 ProblemDetail 响应。详见[下载指南](docs/file-download-guide.zh-CN.md)。

### SDK

- MQ 本体告警补充可空字符串 `alarmTime`，与平台实际发布时间一致，保留原四参构造器。
- 签名的空白字符判定对齐平台，修复全角空格、NBSP 等导致的验签失败，不修改业务请求体。
- HTTP 异常不再附带响应体；JSON 异常省略可能回显原值的 Jackson 消息及原因链，保留错误类型和请求上下文。
- 摄像机模型的 `toString()` 同时隐藏密码和完整流地址，原始访问器保持可用。
- 按当前平台文档和后端核对：新增任务模板启停 `taskTemplate().switchStatus(...)`（平台 v1.3.3+），HTTP 覆盖增至 28 个。
- 机器人控制补充趴下（15）和起立（16）枚举，保留原始指令码入口。
- MQ 任务结果增加可空字符串 `alarmLevel`（平台 v1.3.2+），兼容数字/字符串消息及原有构造器。
- 完善中文 JavaDoc，明确启停副作用、任务计划的可空下发结果及批量取消的部分成功语义。
- 提供七个 API 分组，共 28 个 Station OpenAPI HTTP 调用。
- 支持 Access Token 和 AppKey/SecretKey 签名两种鉴权方式。
- 提供统一分页、异常、请求 ID、超时和受控重试。
- 支持直接向调用方提供的 `OutputStream` 流式下载文件。
- 提供可选的 RocketMQ Router、消息模型和原生 Subscriber。
- RocketMQ 支持任务状态、巡检结果、本体告警和巡视路线四类消息。
- 提供普通 OkHttp 依赖路径和依赖冲突备用包。

### Spring Boot Sample

- 两种 MQ 示例展示本体告警时间；缺失时不补当前时间。
- 摄像机展示视图省略含账号、查询参数、片段或无法解析的流地址，避免 URL 内凭据泄露。
- 启停状态严格接受 JSON 整数，防止小数被截断成控制状态；JSON 解析错误统一返回本地化 ProblemDetail，避免回显请求值。
- 增加模板启停的 Swagger、SDK 调用、双语校验和默认门禁，危险操作增至 11 个；两种 MQ 示例展示告警等级。
- 使用 Spring Boot 3.5.16 和 JDK 17。
- Swagger UI 展示全部 28 个 SDK HTTP 调用。
- 支持 Token 和签名两种配置。
- 11 个写入或控制操作默认锁定。
- 提供中英文参数错误和 SDK 异常响应。
- 文件通过 `StreamingResponseBody` 输出。
- 提供 RocketMQ Starter 主示例和原生 Subscriber 补充示例。

### 获取方式

本项目通过公司 GitHub 公开仓库提供源码。客户可以：

- 把 SDK 源码复制到自己的 Spring Boot 项目；
- 自行构建完整 SDK 模块并发布到企业私有 Nexus。

接入方式见[源码与依赖接入](docs/dependency-and-shade.zh-CN.md)，运行示例见 [Sample README](samples/station-openapi-spring-boot-sample/README.zh-CN.md)。
