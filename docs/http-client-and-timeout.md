# HTTP and Timeout Configuration

**Language:** English | [简体中文](http-client-and-timeout.zh-CN.md)

Most Spring Boot projects only need an endpoint and three timeout values, together with one shared `StationOpenApiClient` bean.

## 1. Endpoint

The endpoint is supplied by technical support and is a complete root address beginning with `http://` or `https://`, for example:

```yaml
station:
  openapi:
    endpoint: http://station.example.com
```

The endpoint may include the context path required by the platform deployment. Do not:

- Append `/remoteApi/time` or another individual operation path;
- Include user information, a password, a query, or a fragment;
- Use an address from a different environment than the credentials.

## 2. Default Timeouts

| Property | Default | Purpose |
|---|---:|---|
| `connect-timeout` | `3s` | Maximum time to establish a network connection |
| `read-timeout` | `10s` | Maximum time to wait for and read response data |
| `call-timeout` | `15s` | Maximum total time for one complete call |

Start with the defaults for normal queries. Increase the read and call timeouts only when large downloads or longer platform processing require it. Every timeout must be positive.

## 3. Reuse a Singleton Client

The Sample's [StationOpenApiConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java) creates a singleton bean and uses `destroyMethod = "close"` to release resources when the application stops.

Inject that bean into business services through their constructors. Do not rebuild a Client in controllers, scheduled tasks, or individual requests, because doing so prevents connection reuse.

## 4. Override Timeouts for One Call

When only one query or download needs more time, override its read and call timeouts with `RequestOptions`:

```java
RequestOptions options = RequestOptions.builder()
        .requestId("large-download-0001")
        .readTimeout(Duration.ofMinutes(2))
        .callTimeout(Duration.ofMinutes(3))
        .build();

client.result().downloadResource(request, outputStream, options);
```

Properties not overridden continue to use the Client defaults. `requestId` correlates logs and exceptions; it is not a business idempotency key.

## 5. Custom OkHttp

Customize OkHttp only when the application must reuse an existing proxy, connection pool, or network interceptor. The available entry points and ownership rules are documented in [OkHttpTransportFactory.java](../station-openapi-sdk-transport-okhttp/src/main/java/com/deeprobotics/station/openapi/sdk/transport/okhttp/OkHttpTransportFactory.java).

Use the SDK's default transport when none of those requirements apply. Do not enable OkHttp automatic retries or redirects, because they can resend write or control requests below the SDK's safety rules.

## 6. Troubleshooting Connectivity

Check these items in order:

1. The endpoint came from technical support for the intended environment;
2. The application host can reach the address and port;
3. No individual operation path was appended to the endpoint;
4. The credentials belong to the same environment;
5. The failure occurred during the connect, read, or call timeout stage.

Use the exception's `requestId`, `operation`, and `code` for troubleshooting. Do not log full request bodies, response bodies, or credentials.
