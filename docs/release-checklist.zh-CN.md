# 接入检查

**语言：** [English](release-checklist.md) | 简体中文

完成 SDK 或 Sample 配置后，可以按本清单确认基础接入是否正确。

## SDK 和项目

- [ ] 项目使用 JDK 17 或更高版本。
- [ ] Spring Boot 使用 3.x。
- [ ] 已选择复制 SDK 源码，或从自己的私有 Nexus 引入完整 SDK 模块。
- [ ] 源码接入时已复制 OkHttp transport 的 `META-INF/services` SPI 文件。
- [ ] 使用的源码或私有制品版本为 `1.0.0-SNAPSHOT`。

## Endpoint 和凭证

- [ ] 已向技术支持获取以 `http://` 或 `https://` 开头的 Endpoint。
- [ ] Endpoint 没有追加具体 `/remoteApi/*` 路径。
- [ ] 已向技术支持获取 Access Token，或 AppKey 和 SecretKey。
- [ ] Token 和签名只启用一种。
- [ ] 真实凭证没有提交到公开仓库或写入日志。

## 第一次调用

- [ ] Spring 中只创建一个 `StationOpenApiClient` Bean。
- [ ] Client 在应用关闭时调用 `close()`。
- [ ] `client.system().getTimeText()` 调用成功。
- [ ] 至少一个 Inventory 查询调用成功。
- [ ] 分页请求使用 `pageNo`。
- [ ] 异常日志只记录 `requestId`、`operation`、`code` 和 `resultUnknown` 等必要字段。

## 可选能力

- [ ] 文件下载使用 `Path` 或 `OutputStream`，没有读取完整 `byte[]`。
- [ ] 写入或控制操作出现 `resultUnknown=true` 时，会先确认状态再决定是否重试。
- [ ] Sample 的 11 个危险操作默认保持锁定，或仅在确认目标后临时启用。
- [ ] 需要 MQ 时，已向技术支持获取 NameServer 并确认 Topic 和消息权限。
- [ ] MQ Handler 能处理重复消息，并且不记录原始消息体。
