# HTTP and Timeout Configuration

**Language:** English | [简体中文](http-client-and-timeout.zh-CN.md)

These settings govern SDK connections to the platform and use the `station.openapi` binding from
the [Spring Boot Quickstart](quickstart.md).

## 1. Endpoint

The endpoint is supplied by technical support and is a complete root address beginning with `http://` or `https://`, for
example:

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

| Property          | Default | Purpose                                                  |
|-------------------|--------:|----------------------------------------------------------|
| `connect-timeout` |    `3s` | Maximum time to establish a network connection           |
| `read-timeout`    |   `10s` | Maximum wait for each blocking read, including downloads |
| `call-timeout`    |   `15s` | Maximum total time for each ordinary JSON HTTP attempt   |

Downloads have no total timeout by default. A file can take longer than 10 or 15 seconds as long as data keeps arriving
within the read timeout. Connection and read timeouts still apply, including while waiting for the response to start.

Client timeout settings and explicit `RequestOptions` overrides must be positive. To impose a total limit on a download,
set `RequestOptions.callTimeout`; the global `call-timeout` does not apply to downloads.

## 3. Reuse a Singleton Client

The
Sample's [StationOpenApiConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java)
creates a singleton bean and uses `destroyMethod = "close"` to release resources when the application stops.

Reuse the bean to share connections. A Client closes only the transport it creates; a transport supplied through
`.transport(...)` remains the caller's responsibility.

## 4. Override Timeouts for One Call

Use `RequestOptions` to allow longer waits between reads or to impose a total limit on one download. This example allows
a two-minute read wait and explicitly limits the download to three minutes:

```java
RequestOptions options = RequestOptions.builder()
        .requestId("large-download-0001")
        .readTimeout(Duration.ofMinutes(2))
        .callTimeout(Duration.ofMinutes(3))
        .build();

client.result().downloadResource(request, outputStream, options);
```

An omitted read timeout inherits the Client setting. An omitted call timeout uses the Client setting for JSON calls and
leaves downloads without a total limit. `requestId` correlates logs and exceptions; it is not a business idempotency
key.

## 5. Custom OkHttp

To reuse a proxy, connection pool, or interceptor, wrap your `OkHttpClient`
with [OkHttpTransportFactory.create (client)](../station-openapi-sdk-transport-okhttp/src/main/java/com/deeprobotics/station/openapi/sdk/transport/okhttp/OkHttpTransportFactory.java)
and pass the transport to `.transport(...)`.

The factory disables automatic retries and redirects. Do not add retry logic in interceptors, which could repeat write
or control operations.

For a custom transport implementation, follow
the [HttpTransport contract](../station-openapi-sdk-core/src/main/java/com/deeprobotics/station/openapi/sdk/http/HttpTransport.java),
including stream ownership and timeout semantics.

## 6. Troubleshooting Connectivity

Check these items in order:

1. The endpoint came from technical support for the intended environment;
2. The application host can reach the address and port;
3. No individual operation path was appended to the endpoint;
4. The credentials belong to the same environment;
5. The failure occurred during the connect, read, or call timeout stage.

Use the exception's `requestId`, `operation`, and `code` for troubleshooting. Do not log full request bodies, response
bodies, or credentials.
