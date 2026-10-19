# Changelog

**Language:** English | [简体中文](CHANGELOG.zh-CN.md)

## 1.0.0-SNAPSHOT

This is the current GitHub source version. It has not been released as a formal version and is not available as a public Maven Central artifact.

### SDK

- Adds `taskTemplate().switchStatus(...)` for platform v1.3.3+, bringing HTTP coverage to 28 operations after checking current documentation and backend sources.
- Adds robot posture commands for lying down (15) and standing up (16), retaining raw command codes.
- Adds nullable string `alarmLevel` to task-result MQ messages (platform v1.3.2+), accepting numeric/string values and retaining the original constructor.
- Clarifies Chinese JavaDoc for template status effects, nullable issue results across execution plans, and partial success during batch cancellation.
- Provides seven API groups with 28 Station OpenAPI HTTP operations.
- Supports Access Token and AppKey/SecretKey signature authentication.
- Provides unified pagination, exceptions, request IDs, timeouts, and controlled retries.
- Supports streaming downloads through `Path` and `OutputStream`.
- Provides optional RocketMQ routing, message models, and a native Subscriber.
- Supports four RocketMQ message types: task status, inspection results, quadruped robot alarms, and inspection routes.
- Provides a normal OkHttp dependency path and a fallback artifact for dependency conflicts.

### Spring Boot Sample

- Requires a JSON integer for template status, preventing fractional values from being truncated into control commands; JSON parsing errors return localized ProblemDetail without echoing input.
- Adds template status Swagger documentation, SDK calls, bilingual validation, and default protection, bringing protected operations to 11; both MQ examples display the alarm level.
- Uses Spring Boot 3.5.16 and JDK 17.
- Exposes all 28 SDK HTTP calls through Swagger UI.
- Supports Token and signature configurations.
- Locks eleven write or control operations by default.
- Provides Chinese and English validation and SDK error responses.
- Streams files through `StreamingResponseBody`.
- Includes a RocketMQ Starter example and a supplemental native Subscriber example.

### Distribution

The project is distributed as source code through the company's public GitHub repository. Customers can:

- Copy SDK source code into a Spring Boot project;
- Build the complete SDK modules and publish them to a private corporate Nexus repository.

See [Source and Dependency Integration](docs/dependency-and-shade.md) for integration and the [Sample README](samples/station-openapi-spring-boot-sample/README.md) for evaluation instructions.
