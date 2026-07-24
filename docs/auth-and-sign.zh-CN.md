# 鉴权与凭证

**语言：** [English](auth-and-sign.md) | 简体中文

Station OpenAPI SDK 支持 Access Token 和 AppKey/SecretKey 签名两种鉴权方式。每个 `StationOpenApiClient` 只能选择一种。

## 1. 先向技术支持获取凭证

开始接入前，请联系项目对接的技术支持人员，获取：

- 当前环境的 Station OpenAPI Endpoint；
- Access Token，或 AppKey 和 SecretKey；
- 凭证可以调用的业务范围。

Endpoint 是以 `http://` 或 `https://` 开头的平台根地址。凭证与环境对应，测试环境和正式环境的凭证不能混用。

## 2. 选择鉴权方式

| 方式 | 需要配置 | SDK 发送的鉴权信息 |
|---|---|---|
| Access Token | `accessToken` | HTTP Header `X-Access-Token` |
| 签名 | `appKey`、`secretKey` | 请求体中的 `appKey`、`timestamp`、`nonce`、`sign` |

使用技术支持实际提供的方式即可。Sample 默认启用 Token profile，也可以切换到签名 profile。

签名模式下，`secretKey` 只在本地参与签名，不会作为请求字段发送。SDK 会为每次请求和重试重新生成签名信息，业务代码不需要自行拼接签名。

Token 模式下，SDK 自动设置 `X-Access-Token`，业务代码不要再通过拦截器重复添加该 Header。

## 3. Spring Boot 配置

完整配置方式见[Spring Boot 快速开始](quickstart.zh-CN.md)。Sample 中可以直接参考：

- [application.yml](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/resources/application.yml)
- [application-token.yml](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/resources/application-token.yml)
- [application-signature.yml](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/resources/application-signature.yml)
- [StationOpenApiConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java)

Token 模式只填写 Access Token。签名模式只填写 AppKey 和 SecretKey。两套凭证同时存在时，Sample 会在启动阶段拒绝启动。

## 4. 签名模式的时间要求

签名包含当前时间戳，因此运行应用的服务器时间需要保持准确。如果 Token 模式可以调用、签名模式持续失败，请先检查系统时间，再确认 AppKey 和 SecretKey 是否属于当前平台环境。

可以使用 `client.system().getTimeText()` 验证基础网络和鉴权是否正常。

## 5. 凭证使用原则

- 不要把真实凭证提交到公开 GitHub 仓库。
- 不要在日志、异常、Swagger 示例或接口响应中输出完整凭证。
- 不要把 `secretKey` 放进业务请求模型。
- 不要让外部调用方通过你的业务接口传入平台凭证。
- 凭证失效、过期或需要更换时，请联系技术支持确认新凭证。
