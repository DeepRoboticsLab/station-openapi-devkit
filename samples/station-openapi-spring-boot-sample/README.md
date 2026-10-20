# Station OpenAPI Spring Boot Sample

**Language:** English | [简体中文](README.zh-CN.md)

This Sample shows how to configure Station OpenAPI SDK in Spring Boot 3, run Swagger UI, call all 28 HTTP operations, and integrate RocketMQ messages.

Use the main module for the first integration. The native MQ module is only for applications that do not use RocketMQ Starter.

> [!WARNING]
> **Demonstration Only — Not for Production**
>
> This Sample exists solely to demonstrate SDK integration. It is not a production-ready application and does not provide the complete authentication, authorization, persistence, auditing, monitoring, high-availability, disaster-recovery, or security controls required by a production system.
>
> **Do not deploy or use the Sample for real production workloads without completing the necessary production design, development, security assessment, and comprehensive testing. Any production incident, unintended device operation, service interruption, data loss, corruption or disclosure, credential exposure, or other direct or indirect loss resulting from such use is solely the user's responsibility. The project provider and maintainers accept no liability.**

## 1. Prerequisites

Prepare the following:

| Item | Requirement |
|---|---|
| JDK | 17 or later |
| Maven | 3.9 or later |
| IDE | IntelliJ IDEA |
| Station endpoint | Root `http://` or `https://` address supplied by technical support |
| Authentication | Access Token, or AppKey and SecretKey |
| RocketMQ | NameServer and message permissions, only when the MQ example is enabled |

Contact your assigned technical support representative before starting to obtain the endpoint and one authentication method. Without them, the Sample can compile but cannot call the Station platform.

Task template enable/disable requires platform **v1.3.3 or later**. Task-result MQ messages provide `alarmLevel` from platform v1.3.2. A missing field remains `null` and must not be treated as normal level 0.

## 2. Import into IDEA

The Sample is an independent Maven project with two modules:

| Module | Purpose |
|---|---|
| `station-openapi-sample-spring-boot` | HTTP, Swagger UI, validation, dangerous-operation protection, and Starter MQ |
| `station-openapi-sample-native-mq` | SDK native RocketMQ Subscriber example |

The Sample depends on the project's `1.1.0` SDK. Before the first run:

1. Open the repository root `pom.xml` in IDEA;
2. Set both Project SDK and Maven Runner JRE to JDK 17;
3. In the Maven tool window, select the root project and run `Lifecycle > install`;
4. After installation, add `samples/station-openapi-spring-boot-sample/pom.xml` in the Maven tool window;
5. Wait for IDEA to finish the Maven reload.

If the customer team has already published the SDK to a private Nexus, make sure the Sample POM version matches the version in that repository.

## 3. Configure the HTTP Sample

The main module uses Token authentication by default. The minimum startup configuration is the endpoint plus the credentials for one mode.

### Token Mode

Open:

```text
station-openapi-sample-spring-boot/src/main/resources/application.yml
```

Confirm that the active profile is `token` and replace the endpoint with the value supplied by technical support:

```yaml
spring:
  profiles:
    active: token

station:
  openapi:
    endpoint: http://station.example.com
```

Then open `application-token.yml` and enter the Access Token:

```yaml
station:
  openapi:
    auth-mode: ACCESS_TOKEN
    access-token: "<Access Token supplied by technical support>"
```

### Signature Mode

Change the active profile in `application.yml` to `signature`:

```yaml
spring:
  profiles:
    active: signature

station:
  openapi:
    endpoint: http://station.example.com
```

Then open `application-signature.yml` and enter:

```yaml
station:
  openapi:
    auth-mode: SIGNATURE
    app-key: "<AppKey supplied by technical support>"
    secret-key: "<SecretKey supplied by technical support>"
```

Configure only the credentials required by the active mode. Token mode must not contain AppKey/SecretKey, and signature mode must not contain an Access Token.

These edits are for local evaluation only. Never commit real credentials to a public repository.

## 4. Run in IDEA

Run the main class:

```text
com.deeprobotics.station.openapi.sample.StationOpenApiSampleApplication
```

After startup, open:

| Page | Address |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

Call this operation first in Swagger:

```text
POST /sample/api/system/get-time-text
```

It does not modify platform data and verifies the endpoint, network, and authentication. After it succeeds, continue with a query in the Inventory group.

## 5. Swagger Operations

Swagger exposes 28 operations:

| Group | Count | Contents |
|---|---:|---|
| Inventory | 8 | Quadruped robots, maps, camera streams, inspection points, maintenance areas, road maps, docks, and nodes |
| Dog | 6 | Status, charging, control, target position, alarms, and real-time position |
| Camera | 2 | Camera control and PTZ reset |
| Task Template | 5 | Create, enable/disable, delete, details, and page query |
| Task | 4 | Issue, control, batch cancellation, and execution page query |
| Result | 2 | Inspection result page query and resource download |
| System | 1 | Platform time |
| **Total** | **28** | |

Each Swagger operation maps to one explicit SDK call. The complete call examples are under:

```text
station-openapi-sample-spring-boot/src/main/java/
  com/deeprobotics/station/openapi/sample/service/
```

The seven services inject `StationOpenApiClient` directly and call the SDK. Use them as references when implementing your own business services.

### Enable or Disable a Task Template

`POST /sample/api/task-template/switch-status` calls `POST /remoteApi/switchTaskModelStatus` and returns `204 No Content` on success. The dangerous-operation switch below protects this operation.

```json
{"taskCode":"TM-001","isEnable":0}
```

`isEnable` must be a JSON integer: 0 disables, 1 enables. Fractional numbers, quoted strings, and booleans return 400. Disabling removes all pending tasks under the template. Enabling does not create tasks automatically; issue the template again. The SDK never retries this write. If the result is unknown, query template details and task executions first.

Direct SDK usage:

```java
client.taskTemplate().switchStatus(
        TaskTemplateStatusRequest.builder().taskCode("TM-001").isEnable(0).build());
```

The request type belongs to `com.deeprobotics.station.openapi.sdk.model.request.tasktemplate`. Template creation and detail responses retain their existing string `isEnable` field.

Robot motion control also provides `MotionCommand.LIE_DOWN` (15) and `STAND_UP` (16). The SDK defaults to `value="0"`; the Sample's `/sample/api/dog/control` requires an explicit value, which can be `"0"` for posture commands. Motion values must still be at most 0.6, and `speed` means 1..5 repeated command submissions.

## 6. Operations Locked by Default

The following eleven operations modify platform data or control field equipment. The Sample returns HTTP 403 before calling the SDK:

| Sample path | Purpose |
|---|---|
| `/sample/api/dog/charge-control` | Control return-to-charge behavior |
| `/sample/api/dog/control` | Control a quadruped robot |
| `/sample/api/dog/set-position` | Send a target position |
| `/sample/api/camera/control` | Control a camera |
| `/sample/api/camera/reset-ptz` | Reset PTZ |
| `/sample/api/task-template/create` | Create a task template |
| `/sample/api/task-template/switch-status` | Enable or disable a task template |
| `/sample/api/task-template/delete-by-codes` | Delete task templates |
| `/sample/api/task/issue` | Issue a task |
| `/sample/api/task/control` | Control a task |
| `/sample/api/task/batch-cancel` | Cancel tasks in a batch |

Enable them only after confirming the target device and test operation. Set this property in `application.yml`:

```yaml
sample:
  dangerous-operations-enabled: true
```

Restart the application after changing it, and restore `false` after the evaluation.

This switch prevents accidental operations in the Sample only. Your own APIs still need business-appropriate access controls.

## 7. Responses and Errors

- Objects, lists, and pages return `200 OK`.
- SDK `void` results and an empty real-time position return `204 No Content`.
- Downloads use `StreamingResponseBody` to forward bytes as they arrive, without temporary files or automatic retries. A failure after response commitment may leave partial output and cannot become a complete ProblemDetail response.
- Downloads have no SDK total timeout by default; connection and read waits remain bounded. The Sample sets `spring.mvc.async.request-timeout=0` to remove the separate MVC asynchronous response limit. This applies to all MVC asynchronous requests and can be overridden through standard Spring configuration. `station.openapi.call-timeout` applies only to ordinary JSON calls; see [File Downloads](../../docs/file-download-guide.md) for per-download limits.
- Validation and SDK errors return `application/problem+json`.
- Pagination requests use `pageNo`, never `pageNum`.
- Camera query responses never expose passwords through Swagger. Stream URLs containing user-info, query parameters, fragments, or invalid syntax become `null` to avoid exposing embedded credentials. Other stream URLs remain visible, and the SDK retains its original accessors.
- `resultUnknown=true` means a write or control result has not been confirmed. Query the state before repeating the operation.

Error messages support `zh_CN` and `en_US`. The `lang` query parameter overrides `Accept-Language`; Chinese is used when no language is specified.

## 8. Enable RocketMQ

The HTTP Sample runs without RocketMQ. Configure MQ only when platform messages are required.

Obtain the NameServer address and message permissions from technical support. See [RocketMQ Message Integration](../../docs/mq-events-guide.md) for configuration.

The main module uses RocketMQ Starter. Its Listener delegates to the SDK's `RemoteMessageRouter` and supports task status, inspection results, quadruped robot alarms, and inspection routes.

## 9. Native MQ Module

`station-openapi-sample-native-mq` demonstrates the SDK native Subscriber only. It does not provide HTTP or Swagger and does not require a Token, AppKey, or SecretKey.

To run it:

1. Enter the NameServer and consumer group in the module's `application.yml`;
2. Set `station.openapi.native-mq.enabled` to `true`;
3. Run `NativeMqSampleApplication` in IDEA.

See [RocketMQ Message Integration](../../docs/mq-events-guide.md) for the complete configuration.

## 10. Key Code Locations

| Content | File |
|---|---|
| SDK properties | [StationOpenApiProperties.java](station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiProperties.java) |
| Singleton Client bean | [StationOpenApiConfiguration.java](station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/config/StationOpenApiConfiguration.java) |
| Seven SDK service groups | [service](station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/) |
| Swagger controllers | [web](station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/web/) |
| Starter MQ Listener | [StarterRemoteMessageListener.java](station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/mq/StarterRemoteMessageListener.java) |
| Native Subscriber bean | [NativeMqConfiguration.java](station-openapi-sample-native-mq/src/main/java/com/deeprobotics/station/openapi/sample/nativemq/NativeMqConfiguration.java) |

## 11. Troubleshooting

### Startup Reports a Missing Endpoint or Credential

Confirm that `application.yml` activates either `token` or `signature`, and that all properties required by that mode are present.

### Startup Reports Mixed Authentication

Clear AppKey and SecretKey in Token mode. Clear the Access Token in signature mode, then restart the application.

### Swagger Opens but Platform Calls Fail

An accessible Swagger page only confirms that the local application started. Check that the endpoint, network, and credentials all belong to the same environment supplied by technical support.

### A Dangerous Operation Returns 403

This is the Sample's default protection. Enable the switch in Section 6 and restart only when an operation must be demonstrated.

### Maven Cannot Resolve the SDK

Run `Lifecycle > install` for the root project in IDEA's Maven tool window, then reload the Sample Maven project.

### MQ Receives No Messages

Confirm that the MQ switch, NameServer, and consumer group are configured. Ask technical support to verify the topic, message permissions, and Broker network.
