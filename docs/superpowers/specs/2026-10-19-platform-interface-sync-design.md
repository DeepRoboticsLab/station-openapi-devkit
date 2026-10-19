# 平台接口同步设计与核对记录

## 目标与范围

按 2026-10-19 本地平台二开文档和后端实现同步 SDK、独立 Spring Boot Sample 及中文 JavaDoc。
用户授权自主完成并在新分支交付，最终由用户联调、审查和合并。本次将原 27 个 HTTP 操作扩展为 28 个，
Sample 默认保护的写入/控制操作从 10 个扩展为 11 个；七个 API 分组、四个 MQ tag 不变。

核对来源：`SDK-Web-Dev/content/zh-CN/inspection/interfaces/`，以及 StationWeb 中的
`controller/remoteApi/RemoteApi.java`、`req/`、`resp/` 和
`controller/dogApi/upload/RemoteHttpUploader.java`。这些目录仅用于核对，自动测试不依赖它们。

## 已确认的差异与处理

| 内容 | 文档/后端事实 | 本次处理 |
|---|---|---|
| 模板启停 | `POST /remoteApi/switchTaskModelStatus`，文档标明平台 v1.3.3 起支持；`taskCode:String`、`isEnable:Integer`（0/1），成功允许空 data | 新增 `TaskTemplateStatusRequest` 与 `taskTemplate().switchStatus(...)` 两个重载，归类 BUSINESS_WRITE |
| 启停副作用 | 停用清除未执行任务；启用不自动下发，需调用 `/issue` | JavaDoc、Swagger、Sample README 明确说明；不自动重试、不自动下发 |
| 运动姿态 | 文档新增 command 15 趴下、16 起立，后端透传控制指令 | 补充 `MotionCommand.LIE_DOWN/STAND_UP`；保留原始数字入口和既有数值限制 |
| MQ 告警等级 | 文档 v1.3.2 新增 `alarmLevel`，上送器实际写入整数，文档写 String | `TaskResultMessage` 增加可空 String 字段，兼容整数/字符串；保留原 20 参数构造器，旧消息保持 null |
| 机器人分页 | 文档 dogs 页写 pageNum；后端明确 pageNo | 继续发送 pageNo，记录文档差异 |
| 运动 value/speed | 文档例子 value=1、称 speed 为速度；后端 type=2 限 value≤0.6，speed 为 1..5 次重复调用 | 保持后端约束，完善示例/JavaDoc，不照抄不合法示例 |
| 下发与批量取消 | issue 数据可能为空；批量取消遇首个失败即停，已执行部分不回滚 | 保持可空结果；补充批量取消的部分成功语义 |
| 文档总数/版本 | 文档总览仍称 27 接口；新接口明确标注 v1.3.3 | 本仓库更新为 28，说明平台版本限制；SDK 仍为 1.0.0-SNAPSHOT |

## 核对覆盖

| 分组 | 路径（均为 /remoteApi 下 POST） | 结果 |
|---|---|---|
| 台账 | dogPage、mapList、streamPage、pointPage、maintainAreaPage、roadMapList、dockList、nodeList | 请求/响应字段与后端对应；pageNo、缺失分页元信息、摄像机密码脱敏规则保留 |
| 机器人 | getDogStateData、chargeCtrl、controlDog、setPosToDog、alarmPage、dogPos | 字段对应；补姿态枚举；位置仍为缓存 JSON 解析后的 Optional，坐标单位见模型注释 |
| 摄像机 | cameraCtrl、ptzReset | 保留原始控制码，参数及设备控制策略对应 |
| 模板 | onlySaveTaskModel、switchTaskModelStatus、removeTaskModelByCodes、taskModelByCode、modelPage | 增加启停，其余请求/响应字段对应；创建/详情 isEnable 为 String，启停/筛选为 Integer |
| 任务 | issue、taskCtrl、batchCancelTask、taskPage | 数组/逗号字符串、日期格式、可空下发结果对应；说明部分取消和筛选默认行为 |
| 结果 | taskPointResultPage、result/resource | nullable confidence、文件列表、流式下载对应 |
| 系统 | time | 保持无业务字段及 ISO 本地时间文本 |
| MQ | uploadHttpRemoteTaskStatus、uploadHttpRemoteResult、uploadHttpRemoteAlarm、uploadHttpRemoteRoute | 仅结果告警等级缺失；坐标仍通过 HTTP 查询 |

## 实现约束

- 沿用 Java 17、Maven 3.9+ 和现有依赖，不升级依赖、不引入后端依赖。
- 新请求使用不可变 Builder 和 LinkedHashMap；必填校验在发起 HTTP 前完成，isEnable 仅允许 0/1。
- 新写入接口只尝试一次；连接/超时失败保持 resultUnknown=true。请求 ID、超时、两种鉴权复用执行器。
- Sample 增加 `/sample/api/task-template/switch-status`，显式调用 SDK；默认 403，启用后成功为 204。
- DTO 提供中文字段说明、双语 Swagger 与本地化校验；不复制签名、重试或 MQ JSON 逻辑。
- 启停状态只接受 JSON 整数，拒绝小数截断、字符串及布尔值转换；JSON 解析错误统一返回本地化 400 ProblemDetail，不回显输入。
- MQ 新字段允许缺失，不将“未知”伪装成“正常”；兼容旧构造器与未知字段。
- 不改外部平台/文档仓库，不提交真实端点、凭据、构建产物或临时设置；用户负责最终合并。

## 验收

先以窄测试复现接口缺失、参数映射和消息字段丢失，再实现；检查默认门禁、参数拒绝、启用路径、
请求 ID、无重试及结果未知。运行 SDK `clean verify`、SDK 安装、Sample `clean verify`，
并生成 Javadoc 验证注释链接和语法。无需真实平台、设备或 RocketMQ。
