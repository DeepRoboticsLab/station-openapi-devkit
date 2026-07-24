# RocketMQ 消息接入

**语言：** [English](mq-events-guide.md) | 简体中文

Station OpenAPI 通过固定 Topic `httpRemote` 推送四类消息。Spring Boot 项目推荐使用 RocketMQ Starter，并把消息交给 SDK 的 `RemoteMessageRouter` 解析和分发。

## 1. 接入前准备

请先联系项目对接的技术支持人员，确认：

- 当前环境的 RocketMQ NameServer 地址；
- Topic `httpRemote` 已经可用；
- 当前应用具有消费消息的权限；
- 应用所在机器能够访问 NameServer 返回的 Broker 地址和端口；
- 如果平台要求预先创建 consumer group，确认可用的 group 名称。

NameServer 通常使用 `host:port` 格式，不是 Station HTTP Endpoint。

## 2. 在 Sample 中启用 Starter MQ

主 Sample 默认关闭 MQ。打开：

```text
samples/station-openapi-spring-boot-sample/
  station-openapi-sample-spring-boot/
    src/main/resources/application.yml
```

把 MQ 配置改为技术支持提供或确认的实际值：

```yaml
station:
  openapi:
    mq:
      enabled: true
      consumer-group: station-openapi-sample

rocketmq:
  name-server: mq-nameserver.example.com:9876
```

然后在 IDEA 中运行 `StationOpenApiSampleApplication`。HTTP 鉴权仍按 Sample README 配置；只启用 MQ 并不能替代 Endpoint 和 Token/签名配置。

消费者启动后再触发一条平台消息。看到对应 Handler 输出的业务摘要日志，才表示消息已经从平台到达 Sample。

## 3. 集成到自己的 Spring Boot 项目

依赖或源码准备见[源码与依赖接入](dependency-and-shade.zh-CN.md)。Starter 版本和依赖组合可直接参考：

- [Sample 主模块 POM](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/pom.xml)
- [Sample 父 POM](../samples/station-openapi-spring-boot-sample/pom.xml)

建议直接参考以下带注释代码：

- [SampleMqConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/SampleMqConfiguration.java)：检查必要配置并创建 Router；
- [StarterRemoteMessageListener.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/mq/StarterRemoteMessageListener.java)：接收 `MessageExt` 并交给 Router；
- [SampleRemoteMessageHandlers.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/mq/SampleRemoteMessageHandlers.java)：处理四类类型安全消息。

Listener 不需要自行解析 JSON，也不要记录原始消息 body。SDK Router 会根据 Tag 转换为对应消息模型。

## 4. 支持的消息

固定 Topic：`httpRemote`。

| Tag | SDK 模型 | 用途 |
|---|---|---|
| `uploadHttpRemoteTaskStatus` | `TaskStatusMessage` | 任务状态和进度 |
| `uploadHttpRemoteResult` | `TaskResultMessage` | 巡检点结果和文件列表 |
| `uploadHttpRemoteAlarm` | `DogAlarmMessage` | 四足狗本体告警 |
| `uploadHttpRemoteRoute` | `RouteMessage` | 任务巡视路线 |

`uploadHttpRemoteDevicePos` 不在 SDK 当前支持的消息范围内。实时坐标通过 `client.dog().getPosition(...)` 查询。

## 5. consumer group

- 同一应用、同一环境的多个实例使用相同 group，由 RocketMQ 分配消息。
- 两个不同应用都需要接收完整消息流时，必须使用不同 group。
- Starter Sample 和原生 MQ Sample 同时运行时必须使用不同 group。
- 不要每次启动随机生成 group。
- 如果 group 需要预先创建，请让技术支持确认后再启动。

## 6. 消息处理

RocketMQ 消息可能重复到达。业务 Handler 应保持线程安全，并根据业务键重复处理：

- 任务结果使用 `taskExecuteCode + devicePointCode` 定位；
- 文件列表按 `fileType + filePath` 合并；
- 处理成功时正常返回；
- 处理失败时抛出异常，让 RocketMQ 稍后重新投递；
- 日志只记录 Tag 和必要的业务标识，不记录原始消息体。

## 7. 原生 Subscriber 示例

只有不使用 RocketMQ Starter 时，才参考 `station-openapi-sample-native-mq`。

在该模块的 `application.yml` 中填写：

```yaml
station:
  openapi:
    native-mq:
      enabled: true
      name-server: mq-nameserver.example.com:9876
      consumer-group: station-openapi-native-sample
```

然后在 IDEA 中运行 `NativeMqSampleApplication`。

原生模块只演示 MQ，不创建 HTTP Client，也不需要 Token 或签名配置。生命周期和四类 Handler 注册见 [NativeMqConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-native-mq/src/main/java/com/deeprobotics/station/openapi/sample/nativemq/NativeMqConfiguration.java)。
