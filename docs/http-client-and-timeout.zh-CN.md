# HTTP 与超时配置

**语言：** [English](http-client-and-timeout.md) | 简体中文

以下配置控制 SDK 到平台的连接，使用[快速开始](quickstart.zh-CN.md)中的 `station.openapi` 属性绑定。

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

| 配置              | 默认值 | 说明                                     |
|-------------------|-------:|------------------------------------------|
| `connect-timeout` |   `3s` | 建立网络连接的最长时间                   |
| `read-timeout`    |  `10s` | 每次阻塞读取的最长等待时间，也适用于下载 |
| `call-timeout`    |  `15s` | 普通 JSON 接口每次 HTTP 尝试的总时限     |

下载默认没有总时限。只要数据持续到达，且每次读取等待未超时，整份文件可以传输超过 10 秒或 15 秒。连接和读取超时仍然生效，包括等待响应开始的阶段。

客户端超时配置和 `RequestOptions` 中显式设置的超时都必须大于 0。需要限制下载总时长时，设置 `RequestOptions.callTimeout`；全局
`call-timeout` 不用于下载。

## 3. 复用单例 Client

Sample
的 [StationOpenApiConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java)
已创建单例 Bean，并通过 `destroyMethod = "close"` 在应用关闭时释放资源。

复用该 Bean 以共享连接。Client 只关闭自行创建的 transport；通过 `.transport(...)` 传入的对象由调用方管理。

## 4. 覆盖单次调用超时

可以通过 `RequestOptions` 放宽读取等待时间，或为某次下载设置总时限。以下示例允许单次读取等待两分钟，并显式限制下载总时长为三分钟：

```java
RequestOptions options = RequestOptions.builder()
        .requestId("large-download-0001")
        .readTimeout(Duration.ofMinutes(2))
        .callTimeout(Duration.ofMinutes(3))
        .build();

client.result().downloadResource(request, outputStream, options);
```

未设置的 read timeout 继承 Client 配置。未设置的 call timeout 对普通 JSON 调用继承 Client 配置，对下载则不设总时限。
`requestId` 用于关联日志和异常，不是业务幂等键。

## 5. 自定义 OkHttp

需要复用代理、连接池或拦截器时，通过 [OkHttpTransportFactory.create (client)](../station-openapi-sdk-transport-okhttp/src/main/java/com/deeprobotics/station/openapi/sdk/transport/okhttp/OkHttpTransportFactory.java)
包装自己的 `OkHttpClient`，再传入 `.transport(...)`。

工厂会关闭自动重试和重定向。不要在拦截器中添加重试逻辑，以免重复执行写入或控制操作。

自行实现 transport
时，须遵守 [HttpTransport 契约](../station-openapi-sdk-core/src/main/java/com/deeprobotics/station/openapi/sdk/http/HttpTransport.java)
，包括输出流归属和超时语义。

## 6. 无法连接时

按以下顺序检查：

1. Endpoint 是否来自当前环境的技术支持；
2. 应用所在机器是否能访问该地址和端口；
3. Endpoint 是否误加了具体接口路径；
4. 凭证是否属于同一环境；
5. 连接是否在 connect、read 或 call timeout 阶段失败。

异常中的 `requestId`、`operation` 和 `code` 可以用于定位问题，但不要记录完整请求体、响应体或凭证。
