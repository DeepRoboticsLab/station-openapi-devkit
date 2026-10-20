# 变更日志

**语言：** [English](CHANGELOG.md) | 简体中文

## v1.1.0

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

`Files.newOutputStream` 会创建或清空目标文件，且要求父目录已存在。不完整文件由应用清理，详见[下载指南](docs/file-download-guide.zh-CN.md)。

### SDK

- 下载默认不设总时限，持续传输不再被普通 JSON 调用的全局总超时截断；连接和读取等待超时继续生效。需要限制下载总时长时，须显式设置正数 `RequestOptions.callTimeout`。自定义 transport 须将 `HttpRequest.callTimeout()` 的 `Duration.ZERO` 解释为无总时限。
- 低层 OkHttp 下载与公共 SDK 默认值统一为读取等待 10 秒、无总时限，删除独立的 60 秒读取回退；移除对同步调用无效的 Dispatcher 并发设置。
- MQ 本体告警补充可空字符串 `alarmTime`（平台 v1.3.3+），保留原四参构造器。
- 签名的空白字符判定对齐平台，修复全角空格、NBSP 等导致的验签失败，不修改业务请求体。
- HTTP 异常不再附带响应体；JSON 异常省略可能回显原值的 Jackson 消息及原因链，保留错误类型和请求上下文。
- 摄像机模型的 `toString()` 同时隐藏密码和完整流地址，原始访问器保持可用。
- 新增任务模板启停 `taskTemplate().switchStatus(...)`（平台 v1.3.3+），HTTP 覆盖增至 28 个。停用会清除未执行任务，启用后须再次下发。
- 机器人控制补充趴下（15）和起立（16）枚举，保留原始指令码入口。
- MQ 任务结果增加可空字符串 `alarmLevel`（平台 v1.3.2+），兼容数字/字符串消息及原有构造器。

### Spring Boot Sample

- 设置 `spring.mvc.async.request-timeout=0`，关闭 `StreamingResponseBody` 使用的 MVC 异步超时，应用可覆盖该配置；响应提交后失败可能留下部分输出，无法再返回完整 ProblemDetail。
- 两种 MQ 示例展示本体告警时间；缺失时不补当前时间。
- 摄像机展示视图省略含账号、查询参数、片段或无法解析的流地址，避免 URL 内凭据泄露。
- 启停状态严格接受 JSON 整数，防止小数被截断成控制状态；JSON 解析错误统一返回本地化 ProblemDetail，避免回显请求值。
- 增加模板启停的 Swagger、SDK 调用、双语校验和默认门禁，危险操作增至 11 个；两种 MQ 示例展示告警等级。

### 文档

- 更新下载、超时、MQ 和 Maven 接入指南，同步中英文示例。
- 完善 JavaDoc，明确启停副作用、任务下发结果可空及批量取消的部分成功语义。

## 1.0.0-SNAPSHOT

初始源码版本，不提供 Maven Central 公共制品。

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
