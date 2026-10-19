# 全程流式下载重构计划

> **For agentic workers:** 使用 superpowers:executing-plans 在当前分支内顺序执行；Sample 保持单人维护。

**Goal:** 下载直接写入调用方 OutputStream，移除 SDK 文件系统操作和下载重试。

**Architecture:** 保留现有 HttpTransport.download 和 OkHttp 分块读写。OpenApiExecutor 单次调用传输层并处理响应、异常，不再暂存或复制文件。ResultApi 仅保留 OutputStream 重载。

**Tech Stack:** Java 17、Maven、Jackson、OkHttp、JUnit 5；无新增依赖。

**Spec:** 本计划下方“已确认设计”；用户已明确同意破坏性变更，要求在 CHANGELOG.md 记录迁移方式。

## 已确认设计

- 正常文件使用现有 8 KiB 缓冲区边读边写，不创建临时文件，也不完整缓存在内存。
- 删除两个 Path 重载；文件创建、覆盖、回滚和清理由调用方负责。
- 下载始终只尝试一次，包括连接失败、超时及部分传输失败；普通查询保留最多两次尝试。
- HTTP 状态和内容类型在写出前由 transport 检查；JSON 错误仍由 executor 解包，有界错误体策略保持不变。
- SDK 关闭自己持有的 HTTP 响应，不关闭调用方 OutputStream。
- 下载连接、超时或输出失败可能留下部分数据，异常携带 requestId/path/operation 和 resultUnknown=true。该标记不计量已写字节，不增加进度追踪包装器。
- 保留现有异常分类；下载失败指引为丢弃不完整输出后重新下载，不使用查询状态或等待 MQ 的控制操作指引。
- Spring Boot 使用现有 StreamingResponseBody，直接转发 SDK 输出；响应提交后的失败无法改为完整 ProblemDetail。

## Global Constraints

- 版本保持 1.0.0-SNAPSHOT，Java 17；六模块 SDK 与独立两模块 Sample 的边界不变。
- 28 个 HTTP 接口、4 个 MQ tag、28 个 Swagger 操作、11 个默认禁用写入/控制操作。
- 不增加策略模式、可选暂存模式、断点续传、下载进度或其他新功能。
- 所有验证使用本地测试替身或 MockWebServer，不需要平台或客户凭证。
- 本次设计覆盖仓库旧文档中的下载重试和 Path 约定；其余契约保持不变。

## Review Focus

- 上游中途断连：调用方已收到部分字节，抛出带上下文异常且不发第二次请求。
- 上游读取超时：不重试，错误说明不引导等待 MQ。
- 输出流写入失败：保持下载异常分类、脱敏和请求上下文，不关闭调用方流。
- JSON/HTML/HTTP 错误：写出前拦截，错误体不能进入文件输出。
- 首批字节可及时到达调用方；大文件不通过内存全量缓存或磁盘中转。

## Task 1: SDK 下载契约与实现

**Files:**
- core: api/ResultApi.java、internal/DefaultResultApi.java、internal/OpenApiExecutor.java。
- core: http/OperationIntent.java、RequestOptions.java、retry/RetryPolicy.java、下载异常与包 JavaDoc。
- tests: OpenApiExecutorTest、ResourceDownloadTest、OkHttpDownloadTest，以及移除 Path 后受影响的调用。

**Interfaces:** 保留 downloadResource(request, OutputStream[, options]) 和 HttpTransport.download(request, OutputStream)；删除 Path 重载和临时文件注入接口。

- [x] 更新“下载不重试、部分字节直接可见、超时、输出失败”的测试并运行，确认旧实现不能满足新契约。
- [x] 删除磁盘暂存、文件移动和下载重试代码，按操作类型调整异常标记及指引。
- [x] 删除旧临时文件测试/替身，补充通过默认 SDK 客户端的 MockWebServer 断流验证。
- [x] 运行 core 与 OkHttp 测试，预期全部通过；保留查询重试和错误体拦截覆盖。

## Task 2: 示例、文档与整体验证

**Files:** CHANGELOG.md、CHANGELOG.zh-CN.md、下载/重试/超时/快速入门/发布清单双语指南、Sample 下载注释及文档。

**Interfaces:** 所有下载用法改为调用方提供 OutputStream；告知失败后可能已有部分输出，不再承诺回滚。

- [x] 清理现行文档中的 Path 调用、自动重试与暂存说明；历史计划作为历史记录保留。
- [x] 在两个 CHANGELOG 中记录移除接口、下载单次尝试、异常语义和调用方迁移示例。
- [x] SDK clean verify；安装 SDK 后运行独立 Sample clean verify；生成两处 JavaDoc。
- [x] 检查最终差异及全部相关引用；复核 SDK 的下载路径没有文件系统操作。
- [x] 提交当前分支，使用带破坏性标记的 Conventional Commit，不推送或合并。

## 执行记录

- 起点：262b4e3，工作区干净；用户已批准本设计，无需再次确认。
- 按直接流传输失败可能已写出数据处理 resultUnknown，不增加逐字节状态跟踪。
- RED：OpenApiExecutorTest 的四项新契约断言失败；MockWebServer 断流测试确认旧实现仍会重试成功。
- GREEN：`mvn -B -ntp -pl station-openapi-sdk-transport-okhttp -am test` 通过，core 151 项、OkHttp 24 项。
- 保留 `prepareOptions` 的包可见性：`DefaultDogApi` 仍需用它关联响应解析异常的请求 ID。
- 整体验证：SDK `clean verify` 197 项通过；安装后 Sample `clean verify` 110 项通过；两处 JavaDoc 生成通过，Sample 仍有原有缺注释警告。
- SDK 独立只读审查：无 Critical、Important 或 Minor 问题；Sample 由主执行者复核，继续直接使用 `StreamingResponseBody`。
- 当前分支 `feat/sync-platform-interfaces` 保留给用户测试和合并；提交主题为 `refactor(sdk)!: 改为全程流式下载`。
