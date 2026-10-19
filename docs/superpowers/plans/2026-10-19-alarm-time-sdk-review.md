# 告警时间与 SDK 复核实施计划

> **For agentic workers:** 使用 superpowers:executing-plans；用户已授权自主执行。Sample 保持单代理，SDK 仅委派只读契约审查。

**Goal:** 补齐 MQ 本体告警时间，复核 SDK 正确性并消除有证据的不必要复杂度。

**Architecture:** 沿用不可变 record、既有 JSON codec 和 MQ router；只追加字段及兼容构造器。

**Tech Stack:** Java 17、Maven、Jackson、JUnit 5、Spring Boot 3。

**Spec:** `docs/superpowers/specs/2026-10-19-alarm-time-sdk-review-design.md`

## 约束与检查重点

- 不升级依赖，不新增端点或 tag，不改外部仓库和真实服务。
- 时间缺失不伪装成当前时间，原四参调用方继续可用。
- 写入/控制不重试；下载不缓冲整个文件或关闭调用方流。
- 参数校验应对应实际契约；异常/日志不得泄露原始报文或凭据。
- 复核时发现的问题先确认根因，再补回归测试，不顺手重构。

## 任务

- [x] 在 `RemoteMessageRouterTest` 验证告警时间传递、缺失/null 和旧构造器，观察缺失字段失败。
- [x] 给 `DogAlarmMessage` 追加 String alarmTime 及兼容构造器，运行 MQ 模块测试。
- [x] 更新 Starter/Native MQ 示例 JavaDoc、消息摘要和双语 MQ 指南/CHANGELOG。
- [x] 对照后端复核 API、模型、签名、MQ、执行器、transport、异常和资源生命周期，记录及修复明确问题。
- [x] 执行 SDK clean verify、SDK install、Sample clean verify 与 JavaDoc，审查 diff 后提交当前分支。

## 执行记录

- 起始工作区干净，继续使用用户待合并的分支 `feat/sync-platform-interfaces`。
- 新字段与后端、文档一致；HTTP 告警时间已有支持，新增范围为 MQ 本体告警。
- 质量复核新增三项修复：平台等价的签名空白判定、相机流地址的展示脱敏、异常省略原始报文。均先用合成数据编写窄失败测试，再实现；具体依据见设计记录。
- SDK core 161 项和 MQ 18 项测试通过；Sample 完整验证通过。全量 SDK 验证中，旧下载断言仍要求输出错误正文，已按新脱敏行为改为断言正文不被回显，保留状态码和请求上下文检查。

## 验证结果

- JDK 17.0.20.1 / Maven 3.9.16。
- SDK `mvn -B -ntp clean verify` 通过：206 项测试，零失败、错误或跳过。
- `mvn -B -ntp -DskipTests install` 后，独立 Sample `clean verify` 通过：Web 104 项、Native MQ 6 项，共 110 项，零失败、错误或跳过。
- SDK 与 Sample JavaDoc 生成通过。新增/修改的契约说明使用中文，原有 Sample 其他入口仍有缺注释警告。
- SDK 契约和增量代码独立只读审查完成；最终 diff 检查通过。没有 POM、依赖或 YAML 变更。
- 未连接平台、设备或 RocketMQ；保留当前分支供用户最终联调与合并。
