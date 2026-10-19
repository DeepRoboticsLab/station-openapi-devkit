# Station OpenAPI Spring Boot Sample

**语言：** [English](README.md) | 简体中文

这个 Sample 用于帮助你在 Spring Boot 3 中配置 Station OpenAPI SDK、运行 Swagger，并查看 28 个 HTTP 接口和 RocketMQ 消息的调用方式。

主模块适合第一次接入；原生 MQ 模块只用于不使用 RocketMQ Starter 的场景。

> [!WARNING]
> **仅用于演示，不可直接用于生产**
>
> 本 Sample 仅用于展示 SDK 接入方式，不是可直接部署的生产应用，也不具备生产系统所需的完整身份认证、权限控制、数据持久化、审计、监控、高可用、容灾和安全防护。
>
> **严禁在未完成必要的生产化设计、开发、安全评估和充分测试前，将 Sample 部署或用于真实生产业务。由此造成的生产事故、设备误操作、服务中断、数据丢失、损坏或泄露、凭证泄露以及其他直接或间接损失，均由使用者自行承担；项目提供方和维护方不承担任何责任。**

## 1. 运行前准备

请准备：

| 项目 | 要求 |
|---|---|
| JDK | 17 或更高版本 |
| Maven | 3.9 或更高版本 |
| 开发工具 | IntelliJ IDEA |
| Station Endpoint | 技术支持提供的 `http://` 或 `https://` 根地址 |
| 鉴权凭证 | Access Token，或 AppKey 和 SecretKey |
| RocketMQ | 仅启用 MQ 示例时需要 NameServer 和消息权限 |

请先联系项目对接的技术支持人员获取 Endpoint 和一种鉴权凭证。没有这些配置，Sample 可以完成编译，但不能调用 Station 平台。

任务模板启停需要平台 **v1.3.3 或更高版本**；MQ 任务结果的 `alarmLevel` 从平台 v1.3.2 起提供。旧消息缺失该字段时为 `null`，不能视为正常等级 0。

## 2. 在 IDEA 中导入

Sample 是独立的 Maven 项目，包含：

| 模块 | 用途 |
|---|---|
| `station-openapi-sample-spring-boot` | HTTP、Swagger UI、参数校验、危险操作保护和 Starter MQ |
| `station-openapi-sample-native-mq` | SDK 原生 RocketMQ Subscriber 示例 |

Sample 依赖本项目的 `1.0.0-SNAPSHOT` SDK。第一次运行前：

1. 使用 IDEA 打开仓库根 `pom.xml`；
2. 把 Project SDK 和 Maven Runner JRE 都设置为 JDK 17；
3. 在 Maven 工具窗口中找到根项目，双击 `Lifecycle > install`；
4. 安装完成后，在 Maven 工具窗口添加 `samples/station-openapi-spring-boot-sample/pom.xml`；
5. 等待 IDEA 完成 Maven Reload。

如果 SDK 已经由客户团队发布到自己的 Nexus，只需确保 Sample POM 中的版本与 Nexus 中的版本一致。

## 3. 配置 HTTP Sample

主模块默认使用 Token 模式。最低启动配置只有 Endpoint 和对应凭证。

### Token 模式

打开：

```text
station-openapi-sample-spring-boot/src/main/resources/application.yml
```

确认 profile 为 `token`，并把 Endpoint 改为技术支持提供的地址：

```yaml
spring:
  profiles:
    active: token

station:
  openapi:
    endpoint: http://station.example.com
```

再打开 `application-token.yml`，填写 Access Token：

```yaml
station:
  openapi:
    auth-mode: ACCESS_TOKEN
    access-token: "<技术支持提供的 Access Token>"
```

### 签名模式

把 `application.yml` 中的 profile 改为 `signature`：

```yaml
spring:
  profiles:
    active: signature

station:
  openapi:
    endpoint: http://station.example.com
```

再打开 `application-signature.yml`，填写：

```yaml
station:
  openapi:
    auth-mode: SIGNATURE
    app-key: "<技术支持提供的 AppKey>"
    secret-key: "<技术支持提供的 SecretKey>"
```

只配置当前模式需要的凭证。Token 模式不要填写 AppKey/SecretKey，签名模式不要填写 Access Token。

以上修改只用于本地运行，不要把真实凭证提交到公开仓库。

## 4. 在 IDEA 中运行

运行主类：

```text
com.deeprobotics.station.openapi.sample.StationOpenApiSampleApplication
```

启动成功后打开：

| 页面 | 地址 |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

建议先在 Swagger 中调用：

```text
POST /sample/api/system/get-time-text
```

它不会修改平台数据，可以验证 Endpoint、网络和鉴权。成功后再调用 Inventory 分组中的查询接口。

## 5. Swagger 中的功能

Swagger 共提供 28 个操作：

| 分组 | 数量 | 内容 |
|---|---:|---|
| Inventory | 8 | 四足狗、地图、摄像机流、巡检点、维保区域、路网、停靠点和节点查询 |
| Dog | 6 | 状态、充电、控制、选点、告警和实时坐标 |
| Camera | 2 | 摄像机控制和云台复位 |
| Task Template | 5 | 创建、启停、删除、详情和分页 |
| Task | 4 | 下发、控制、批量取消和执行记录分页 |
| Result | 2 | 巡检结果分页和资源下载 |
| System | 1 | 平台时间 |
| **合计** | **28** | |

每个 Swagger 操作都对应一个明确的 SDK 调用。完整调用代码位于：

```text
station-openapi-sample-spring-boot/src/main/java/
  com/deeprobotics/station/openapi/sample/service/
```

七个 Service 直接注入 `StationOpenApiClient` 并调用 SDK，适合复制到自己的业务 Service 中再按需求调整。

### 任务模板启停

`POST /sample/api/task-template/switch-status` 映射到 `POST /remoteApi/switchTaskModelStatus`，成功返回 `204 No Content`。它受下节的危险操作开关保护。

```json
{"taskCode":"TM-001","isEnable":0}
```

`isEnable` 必须为 JSON 整数：0 停用、1 启用；小数、带引号的字符串和布尔值会返回 400。停用会清除模板下所有未执行任务；启用不会自动生成任务，必须再次下发。该操作不重试，结果未知时先查询模板详情和任务执行记录。

直接使用 SDK 时：

```java
client.taskTemplate().switchStatus(
        TaskTemplateStatusRequest.builder().taskCode("TM-001").isEnable(0).build());
```

请求类型位于 `com.deeprobotics.station.openapi.sdk.model.request.tasktemplate`。与此接口不同，模板创建和详情的 `isEnable` 保持字符串类型。

机器人运动控制现在也提供 `MotionCommand.LIE_DOWN`（15）和 `STAND_UP`（16）。SDK 默认 `value="0"`；Sample 的 `/sample/api/dog/control` 要求显式提供 `value`，姿态指令可传 `"0"`。运动 `value` 仍不得大于 0.6，`speed` 表示 1..5 次重复下发。

## 6. 默认锁定的操作

以下 11 个操作会修改平台数据或控制现场设备，因此在 Sample 中默认返回 403，不会调用 SDK：

| Sample 路径 | 用途 |
|---|---|
| `/sample/api/dog/charge-control` | 控制返航充电 |
| `/sample/api/dog/control` | 控制四足狗 |
| `/sample/api/dog/set-position` | 下发目标位置 |
| `/sample/api/camera/control` | 控制摄像机 |
| `/sample/api/camera/reset-ptz` | 复位云台 |
| `/sample/api/task-template/create` | 创建任务模板 |
| `/sample/api/task-template/switch-status` | 启用或停用任务模板 |
| `/sample/api/task-template/delete-by-codes` | 删除任务模板 |
| `/sample/api/task/issue` | 下发任务 |
| `/sample/api/task/control` | 控制任务 |
| `/sample/api/task/batch-cancel` | 批量取消任务 |

只有在已经确认目标设备和测试内容时，才把 `application.yml` 中的配置改为：

```yaml
sample:
  dangerous-operations-enabled: true
```

修改后重新运行应用。演示完成后改回 `false`。

这个开关只用于 Sample 防止误操作。客户自己的接口仍需根据业务要求控制谁可以执行这些操作。

## 7. 返回和异常

- 对象、列表和分页正常返回 `200 OK`。
- SDK `void` 和空的实时坐标返回 `204 No Content`。
- 文件下载使用 `StreamingResponseBody` 流式输出。
- 校验和 SDK 异常返回 `application/problem+json`。
- 分页请求使用 `pageNo`，不要使用 `pageNum`。
- 摄像机查询结果不会向 Swagger 返回密码；含账号、查询参数、片段或无法解析的流地址会置为 `null`，避免 URL 内的凭据泄露。无这些内容的流地址正常展示，SDK 原始访问器保持可用。
- `resultUnknown=true` 表示写入或控制结果尚未确认，应先查询状态，不要立即重复调用。

错误消息支持 `zh_CN` 和 `en_US`。查询参数 `lang` 的优先级高于 `Accept-Language`，未指定时使用中文。

## 8. 启用 RocketMQ

HTTP Sample 不需要 RocketMQ 就能运行。只有需要接收平台消息时，才配置 MQ。

NameServer 地址和消息权限需要向技术支持获取。配置步骤见[RocketMQ 消息接入](../../docs/mq-events-guide.zh-CN.md)。

主模块使用 RocketMQ Starter；Listener 会把消息交给 SDK `RemoteMessageRouter`，支持任务状态、巡检结果、本体告警和巡视路线四类消息。

## 9. 原生 MQ 模块

`station-openapi-sample-native-mq` 只演示 SDK 原生 Subscriber，不提供 HTTP 或 Swagger，也不需要 Token、AppKey 或 SecretKey。

如需运行：

1. 在该模块的 `application.yml` 中填写 NameServer 和 consumer group；
2. 把 `station.openapi.native-mq.enabled` 改为 `true`；
3. 在 IDEA 中运行 `NativeMqSampleApplication`。

完整配置见[RocketMQ 消息接入](../../docs/mq-events-guide.zh-CN.md)。

## 10. 关键代码入口

| 内容 | 文件 |
|---|---|
| SDK 配置属性 | [StationOpenApiProperties.java](station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiProperties.java) |
| 单例 Client Bean | [StationOpenApiConfiguration.java](station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java) |
| 七组 SDK 调用 | [service](station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/) |
| Swagger Controller | [web](station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/web/) |
| Starter MQ Listener | [StarterRemoteMessageListener.java](station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/mq/StarterRemoteMessageListener.java) |
| 原生 Subscriber Bean | [NativeMqConfiguration.java](station-openapi-sample-native-mq/src/main/java/com/deeprobotics/station/openapi/sample/nativemq/NativeMqConfiguration.java) |

## 11. 常见问题

### 应用启动时提示 Endpoint 或凭证缺失

确认 `application.yml` 激活了 `token` 或 `signature`，并填写了该模式要求的全部配置。

### 应用提示混合鉴权

Token 模式清空 AppKey 和 SecretKey；签名模式清空 Access Token，然后重新运行。

### Swagger 能打开，但平台调用失败

Swagger 能打开只说明本地应用已启动。继续确认 Endpoint、网络和凭证是否都属于技术支持提供的同一环境。

### 危险接口返回 403

这是 Sample 的默认保护。确实需要演示时，按第 6 节启用开关并重新运行。

### Maven 无法解析 SDK

先在 IDEA 的根项目 Maven 工具窗口执行 `Lifecycle > install`，再 Reload Sample Maven 项目。

### MQ 没有消息

确认 MQ 开关、NameServer 和 consumer group 已填写，并让技术支持确认 Topic、消息权限和 Broker 网络。
