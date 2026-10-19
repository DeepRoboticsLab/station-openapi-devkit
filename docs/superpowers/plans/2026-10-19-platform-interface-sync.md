# 平台接口同步实施计划

> **For agentic workers:** 使用 superpowers:executing-plans 在当前分支单代理执行；用户已授权自主取舍，不逐项审批。

**Goal:** 同步 28 个 HTTP 操作、11 个默认受保护的示例操作及四类 MQ 消息。

**Architecture:** 沿用请求 Builder、统一执行器、薄 Controller/Service 和 RemoteMessageRouter。
新增字段保持向前兼容；不复制后端 DTO，不改变鉴权和重试边界。

**Tech Stack:** Java 17、Maven 3.9+、Jackson、JUnit 5、Spring Boot 3。

**Spec:** `docs/superpowers/specs/2026-10-19-platform-interface-sync-design.md`

## 全局约束

- 版本仍为 1.0.0-SNAPSHOT，Sample 独立于 SDK reactor。
- BUSINESS_WRITE/DEVICE_CONTROL 不重试；凭据和原始报文不进入日志及异常。
- 保留现有公开入口；不使用真实外部服务；只在新分支提交本次工作。

## 审查重点

- 启停入参 null、空白 taskCode、isEnable 缺失或越界应在 I/O 前拒绝。
- 启停超时不得重试，resultUnknown 必须为 true。
- Sample 默认关闭、开启成功、无效请求和请求 ID 传递均应验证。
- MQ alarmLevel 整数/字符串、缺失/空值和旧构造器兼容。
- 姿态指令的真实请求映射及默认 value=0，保持原数值上限。

## 任务

- [x] SDK：先更新 OpenApiCoverageTest 并观察缺失启停路径失败；新增 TaskTemplateApiTest 的请求体、空结果、参数校验、超时断言；实现 TaskTemplateStatusRequest、TaskTemplateApi、DefaultTaskTemplateApi；加入 PublicApiInputValidationTest。
- [x] 运动：在 DogApiTest 覆盖 15/16 的请求映射；补充枚举和中文 JavaDoc；运行 core 测试。
- [x] MQ：RemoteMessageRouterTest 先覆盖 alarmLevel 解析及历史消息；扩展 TaskResultMessage 并保留旧构造器；运行 rocketmq reactor 测试。
- [x] Sample：先更新 SampleHttpCoverageTest、DangerousOperationCoverageTest 并观察失败；增加启用后真实 SDK 调用的 HTTP 测试；实现 DTO、Service、Controller、双语校验/Swagger；补充消息告警等级用法。
- [x] 文档：同步 AGENTS、双语 README/CHANGELOG、MQ 指南及当前接口数量；补充新增功能的版本限制和副作用说明，不改历史文档。
- [x] 验证：SDK clean verify → SDK install → Sample clean verify → Javadoc；审查最终 diff 并提交至 feat/sync-platform-interfaces，留待用户合并。

## 执行记录

- 初始工作区干净；分支已创建。JDK 使用本机 Temurin 17.0.20.1；基线 SDK clean verify 通过。
- 决策：用户要求自主完成，且 Sample 规则要求单代理，因此本次全部工作在当前分支单代理执行。
- 决策：后端代码决定 wire 契约；已确认的文档差异记录在设计中，不改外部仓库。
- 审查修正：新增回归测试复现 Jackson 将 0.5/1.9 截断为 0/1，故仅对启停字段启用严格整数反序列化；JSON 解析异常统一转为不回显输入的双语校验 ProblemDetail。
- 本机 AGENTS.md 被既有 .gitignore 排除；已同步本地契约数量和新增规则，保留忽略策略，不将该本地维护文件强制加入提交。

## 验证结果

- JDK 17.0.20.1 / Maven 3.9.16；SDK `mvn -B -ntp clean verify` 通过，201 项测试（含 shaded 集成测试），无失败或跳过。
- `mvn -B -ntp -DskipTests install` 通过；Sample `mvn -B -ntp -f samples/station-openapi-spring-boot-sample/pom.xml clean verify` 通过，Web 100 项、Native MQ 6 项测试，无失败或跳过。
- SDK `mvn -B -ntp -DskipTests javadoc:javadoc` 和 Sample 的 `org.apache.maven.plugins:maven-javadoc-plugin:3.8.0:javadoc` 均通过。Sample 原有公共方法仍有缺注释提示，本次变更的契约与新增入口已补中文说明；未为隐藏警告修改构建配置。
- 本地只读核对：文档、后端、SDK 各 28 个 HTTP 路径，集合一致；27 个业务请求 DTO、15 个响应 DTO 未发现字段遗漏。时间接口没有业务请求 DTO。
- 完整 diff 自审及 `git diff --check` 通过；依赖/POM/YAML 未变更。四个 MQ tag、摄像机脱敏、默认门禁、签名和 token、重试、流式下载、SPI/shading 均由现有及新增测试验证。
- 未连接真实平台、设备或 RocketMQ，未修改外部源码/文档仓库；分支交由用户测试与合并。
