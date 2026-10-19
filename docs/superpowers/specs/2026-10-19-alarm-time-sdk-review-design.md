# 告警时间同步与 SDK 质量复核

## 范围与依据

在 `feat/sync-platform-interfaces` 上继续同步本地最新二开文档，并复核 SDK 与平台实际实现。
此次增量从 `32c5993` 开始；既有接口同步计划已经完成，不重新执行。

- 文档：`SDK-Web-Dev/content/zh-CN/inspection/interfaces/messages/robot-alarm.md`。
- 平台：`DeviceAlarmReq.alarmTime` 为 String，`TcpQueryAssembler.buildRemoteAlarmReq` 使用
  `LocalDateTimeUtil.formatNormal(dogAlarm.getAlarmTime())`，由 `RemoteHttpUploader.uploadAlarm` 发布。
- HTTP `alarmPage` 的 `DogAlarmResp.alarmTime` 为 LocalDateTime，SDK `DogAlarmInfo` 已支持。

## 实现决策

- `DogAlarmMessage` 追加可空 String `alarmTime`，格式 `yyyy-MM-dd HH:mm:ss`，保持 MQ 模型的时间文本惯例。
- 保留原四参构造器，旧消息缺失或显式 null 时保持 null，不用消费时间补齐，不另造时间解析机制。
- 更新两个 MQ 示例处理器及中文 JavaDoc，说明时间不携带时区。HTTP 告警模型无需改动。
- 保持 28 个 HTTP 操作、四个 MQ tag、11 个默认禁用的示例操作及现有依赖。

## 质量复核方法

检查 API 与实际请求/响应、签名、重试、流式下载、资源所有权、MQ 路由和依赖边界。
以具体可复现的问题决定修复，不为减少类数量而破坏公开 API，也不增加没有平台依据的校验或抽象。
新增行为先以窄测试观察失败，再做最小实现。测试只用离线数据与本地传输替身。
Sample 由主代理完成；SDK 契约另作只读审查。

## 验收

验证新消息、旧消息、null 和旧构造器兼容；执行 SDK `clean verify`、安装 SDK、Sample `clean verify`
以及 JavaDoc 生成。最终记录具体发现、保留的设计理由与验证边界，提交留待用户测试和合并。

## 复核发现与处理

| 发现 | 依据与影响 | 最小修复 |
|---|---|---|
| MQ 本体告警丢失时间 | 文档和实际 `DeviceAlarmReq` 均含 alarmTime，SDK 原模型未声明 | 追加字段、保留旧构造器；新消息与缺失/null 均验证 |
| 空白值参与签名的规则不一致 | 平台 `SignUtils` 使用 Hutool 5.7.20 的空白判定，SDK 原来使用 trim；全角空格/NBSP 会导致签名不同，U+0001 又会被错误忽略 | 用 JDK 字符判断复现平台规则，不增加 Hutool 依赖；固定 MD5 向量覆盖正反例 |
| 相机流地址经日志和示例响应输出 | 平台直接复制 streamUrl，URL 可含账号密码或查询令牌；原来只隐藏独立 password 字段 | SDK toString 整体隐藏 URL；Sample 只展示可解析且不含 user-info/query/fragment 的 URL，其他返回 null；SDK 访问器保留原值 |
| 异常包含报文或无字段名标记的值 | HTTP 错误体截断后仍被拼进异常；Jackson 格式错误会单独回显属性值，字段名脱敏无法覆盖 | 省略 HTTP 错误体和 Jackson 原始消息/原因链，保留错误类型、状态码和请求上下文；移除 bodySnippet 加工代码 |

上述问题均使用合成内容复现，没有读取真实凭据或连接外部服务。HTTP 异常原构造器签名保留，
其 bodySnippet 参数现被忽略；JSON 异常不再提供原始 Jackson 原因链，避免日志再次展开原值。
业务错误仍按既有规范提供脱敏后的平台消息。

## 复核覆盖及复杂度结论

- 逐组复核 28 个 HTTP 路径及请求/响应模型、四类 MQ 发布 DTO 和映射，未发现其他待修复的字段差异。
- 签名、Token、分页、可空结果、原始命令码、下载 octet-stream 与当前后端一致。
- 重试集中于执行器；写入/控制单次尝试，OkHttp 禁止透明重放，下载失败不拼接半截内容。
- SDK 自建 transport 负责关闭，客户注入对象及输出流由客户持有；SPI、shading 和模块依赖边界维持。
- MQ 双层 JSON 来自实际发布方式，dogPos 二次解析来自后端 R<String>，均有实际依据。
- 下载临时文件用于重试隔离和目标文件完整性；集合副本用于稳定请求快照。没有为了减少代码量移除这些必要行为。
- 未增加通用脱敏框架、时间转换器、业务缓存或新的依赖。保留有明确契约或失败场景支撑的校验；未发现需要额外抽象的重复逻辑。

SDK 契约及最终 SDK 增量均经过独立只读审查；Sample 由主代理单独完成。
结论限于本地源码对照和离线测试，真实平台、设备及 MQ 的运行环境仍需用户联调验收。
