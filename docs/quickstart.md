# Spring Boot Quickstart

**Language:** English | [简体中文](quickstart.zh-CN.md)

This guide shows how to integrate Station OpenAPI SDK into an existing Spring Boot 3 application and complete the first
platform call. The minimum SDK runtime is JDK 17.

## 1. Obtain Platform Configuration

Contact your assigned technical support representative to obtain:

- The Station OpenAPI root endpoint beginning with `http://` or `https://`;
- Either an AppKey and SecretKey pair or an Access Token;
- The business operations permitted by those credentials.

The endpoint may include a deployment context path, but do not append an individual `/remoteApi/*` path. Template
enable/disable requires platform v1.3.3+; see [RocketMQ Message Integration](mq-events-guide.md) for MQ field
availability.

## 2. Add the SDK to the Project

The SDK is distributed as GitHub source code and is not published to Maven Central. Choose one method:

- **Source integration**: copy SDK core, the OkHttp transport, and the SPI resources into your project.
- **Maven dependency**: install the SDK locally or publish it to a private Nexus repository, then depend on
  `1.1.0`.

See [Source and Dependency Integration](dependency-and-shade.md) for setup. Once the SDK is available in your local or
private Maven repository, add:

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 3. Create the Spring Boot Configuration

Copy these two Sample classes into a package scanned by your Spring Boot application:

- [StationOpenApiProperties.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiProperties.java)
- [StationOpenApiConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java)

They bind properties, validate configuration at startup, and create a singleton `StationOpenApiClient` with
`destroyMethod="close"`. Reuse this bean for all calls.

## 4. Configure One Authentication Mode

The Sample separates common settings and the two authentication modes into three configuration files. Your application
can use the same structure.

### Token Mode

`application.yml`:

```yaml
spring:
  profiles:
    active: token

station:
  openapi:
    endpoint: ${STATION_ENDPOINT:}
    connect-timeout: 3s
    read-timeout: 10s
    call-timeout: 15s # Ordinary JSON calls only
```

`application-token.yml`:

```yaml
spring:
  config:
    activate:
      on-profile: token

station:
  openapi:
    auth-mode: ACCESS_TOKEN
    access-token: ${STATION_ACCESS_TOKEN:}
```

### Signature Mode

Change `spring.profiles.active` in `application.yml` to `signature`, then add `application-signature.yml`:

```yaml
spring:
  config:
    activate:
      on-profile: signature

station:
  openapi:
    auth-mode: SIGNATURE
    app-key: ${STATION_APP_KEY:}
    secret-key: ${STATION_SECRET_KEY:}
```

Set `STATION_ENDPOINT` and the credential environment variables for the selected mode. Enable exactly one of `token` and
`signature`, and configure only its credentials. Never commit real credentials to version control.

See [Authentication and Credentials](auth-and-sign.md) for details.

## 5. Complete the First Call

Inject the singleton Client into a business service and begin with the platform time operation, which does not modify
platform data:

```java
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import org.springframework.stereotype.Service;

@Service
public class StationConnectionService {

    private final StationOpenApiClient client;

    public StationConnectionService(StationOpenApiClient client) {
        this.client = client;
    }

    public String getPlatformTime() {
        return client.system().getTimeText();
    }
}
```

For the seven API groups, see the request construction and calls in
the [Sample services](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/).

## 6. Handle Results

- Pagination uses `pageNo`, starting at 1. Do not use `pageNum`.
- Catch `StationOpenApiException` to handle HTTP SDK failures. You may log its `requestId`, `operation`, `code`, and
  `resultUnknown`.
- Never log complete request bodies, response bodies, or credentials.
- If a write or control operation fails with `resultUnknown=true`, query the state or wait for an MQ message before
  deciding whether to repeat it.
- Download files through `OutputStream`. Downloads make one attempt and may leave partial output on failure.

See [Retries and Result Confirmation](retry-and-idempotency.md) and [File Downloads](file-download-guide.md) for
detailed behavior.

## 7. Run the Complete Sample

To inspect all 28 operations, request parameters, and response structures, run
the [Spring Boot Sample](../samples/station-openapi-spring-boot-sample/README.md) in IDEA and use Swagger UI.
