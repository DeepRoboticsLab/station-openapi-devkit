# HTTP 与超时配置

**语言：** [English](http-client-and-timeout.md) | 简体中文

大多数 Spring Boot 项目只需配置 Endpoint 和三个超时，并在整个应用中复用一个 `StationOpenApiClient` Bean。

## 1. Endpoint

Endpoint 由技术支持提供，使用以 `http://` 或 `https://` 开头的完整根地址，例如：

```yaml
station:
  openapi:
    endpoint: http://station.example.com
```

Endpoint 可以包含平台部署所需的 context path，但不要：

- 追加 `/remoteApi/time` 或其他具体接口路径；
- 添加用户名、密码、query 或 fragment；
- 使用与凭证不属于同一环境的地址。

## 2. 默认超时

| 配置 | 默认值 | 说明 |
|---|---:|---|
| `connect-timeout` | `3s` | 建立网络连接的最长时间 |
| `read-timeout` | `10s` | 等待和读取响应的最长时间 |
| `call-timeout` | `15s` | 一次完整调用的总时间 |

普通查询先使用默认值。下载较大文件或平台处理时间较长时，再根据实际情况增加 read 和 call timeout。所有超时都必须大于 0。

## 3. 复用单例 Client

Sample 的 [StationOpenApiConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java) 已创建单例 Bean，并通过 `destroyMethod = "close"` 在应用关闭时释放资源。

业务 Service 通过构造器注入该 Bean。不要在 Controller、定时任务或每次请求中重新构建 Client，否则无法复用连接。

## 4. 单次调用使用更长超时

只在某一次查询或下载需要更长时间时，可以通过 `RequestOptions` 覆盖 read 和 call timeout：

```java
RequestOptions options = RequestOptions.builder()
        .requestId("large-download-0001")
        .readTimeout(Duration.ofMinutes(2))
        .callTimeout(Duration.ofMinutes(3))
        .build();

client.result().downloadResource(request, target, options);
```

未覆盖的配置继续使用 Client 默认值。`requestId` 用于关联日志和异常，不是业务幂等键。

## 5. 自定义 OkHttp

只有项目需要复用自己的代理、连接池或网络拦截器时，才需要自定义 OkHttp。相关入口和生命周期说明见 [OkHttpTransportFactory.java](../station-openapi-sdk-transport-okhttp/src/main/java/com/deeprobotics/station/openapi/sdk/transport/okhttp/OkHttpTransportFactory.java)。

没有这些需求时使用 SDK 默认 transport 即可。不要开启 OkHttp 自动重试或自动重定向，以免写入和控制请求被底层重复发送。

## 6. 无法连接时

按以下顺序检查：

1. Endpoint 是否来自当前环境的技术支持；
2. 应用所在机器是否能访问该地址和端口；
3. Endpoint 是否误加了具体接口路径；
4. 凭证是否属于同一环境；
5. 连接是否在 connect、read 或 call timeout 阶段失败。

异常中的 `requestId`、`operation` 和 `code` 可以用于定位问题，但不要记录完整请求体、响应体或凭证。
