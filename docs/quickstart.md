# Spring Boot Quickstart

**Language:** English | [简体中文](quickstart.zh-CN.md)

This guide shows how to integrate Station OpenAPI SDK into an existing Spring Boot 3 application and complete the first platform call. The minimum SDK runtime is JDK 17.

## 1. Obtain Platform Configuration

Contact your assigned technical support representative to obtain:

- The Station OpenAPI root endpoint beginning with `http://` or `https://`;
- Either an AppKey and SecretKey pair or an Access Token;
- The business operations permitted by those credentials.

Confirm that the target environment supports the features you need: template enable/disable requires platform v1.3.3+, and MQ result alarm levels are available from v1.3.2. The endpoint may include a context path required by the deployment, but do not append an individual `/remoteApi/*` path.

## 2. Add the SDK to the Project

The SDK is distributed as GitHub source code and is not published to Maven Central. Choose one method:

- **Source integration**: copy SDK core, the OkHttp transport, and the SPI resources into your project.
- **Private Nexus**: build and publish the complete SDK modules, then depend on `1.0.0-SNAPSHOT`.

See [Source and Dependency Integration](dependency-and-shade.md) for the exact directories and dependencies. If the SDK has already been published to your private Nexus, add:

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

This dependency resolves only after your private Nexus contains the complete SDK modules.

## 3. Create the Spring Boot Configuration

The Sample provides complete property binding and a singleton Client bean:

- [StationOpenApiProperties.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiProperties.java)
- [StationOpenApiConfiguration.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java)

Both classes include explanatory comments, startup validation, and Client shutdown configuration. Copy them into your Spring Boot project and adjust the package declaration to match your application.

Keep `StationOpenApiClient` as a singleton bean. Do not create a new Client in controllers, services, or individual business calls.

## 4. Configure One Authentication Mode

The Sample separates common settings and the two authentication modes into three configuration files. Your application can use the same structure.

### Token Mode

`application.yml`:

```yaml
spring:
  profiles:
    active: token

station:
  openapi:
    endpoint: http://station.example.com
    connect-timeout: 3s
    read-timeout: 10s
    call-timeout: 15s
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
    access-token: "<Access Token supplied by technical support>"
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
    app-key: "<AppKey supplied by technical support>"
    secret-key: "<SecretKey supplied by technical support>"
```

Never enable both modes. Do not configure AppKey/SecretKey in Token mode, and do not configure an Access Token in signature mode. Store real credentials only in local configuration or the credential management system used by your project. Never commit them to a public repository.

See [Authentication and Credentials](auth-and-sign.md) for details.

## 5. Complete the First Call

Inject the singleton Client into a business service and begin with the platform time operation, which does not modify platform data:

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

After that call succeeds, use the seven Sample services as references for the required business groups:

- [InventorySampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/InventorySampleService.java)
- [DogSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/DogSampleService.java)
- [CameraSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/CameraSampleService.java)
- [TaskTemplateSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/TaskTemplateSampleService.java)
- [TaskSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/TaskSampleService.java)
- [ResultSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/ResultSampleService.java)
- [SystemSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/SystemSampleService.java)

These classes build explicit SDK requests and call the public APIs without any additional backend dependencies.

## 6. Handle Results

- Pagination uses `pageNo`, starting at 1. Do not use `pageNum`.
- Every SDK exception extends `StationOpenApiException`. You may log its `requestId`, `operation`, `code`, and `resultUnknown`.
- Never log complete request bodies, response bodies, or credentials.
- If a write or control operation fails with `resultUnknown=true`, query the state or wait for an MQ message before deciding whether to repeat it.
- Download files through the `Path` or `OutputStream` APIs.

See [Retries and Result Confirmation](retry-and-idempotency.md) and [File Downloads](file-download-guide.md) for detailed behavior.

## 7. Run the Complete Sample

To inspect all 28 operations, request parameters, and response structures, run the [Spring Boot Sample](../samples/station-openapi-spring-boot-sample/README.md) in IDEA and use Swagger UI.
