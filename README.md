# Station OpenAPI Java SDK

**Language:** English | [简体中文](README.zh-CN.md)

Station OpenAPI Java SDK is intended for applications built with JDK 17 and Spring Boot 3. It provides 28 HTTP operations, two authentication modes, unified pagination and exception handling, streaming file downloads, and optional RocketMQ message integration.

The current source version is `1.1.0`. The project is distributed as source code through the company's public GitHub repository. No official JARs or public Maven coordinates are published to Maven Central.

## Prerequisites

Prepare the following before integration:

| Item | Requirement |
|---|---|
| JDK | 17 or later |
| Spring Boot | 3.x |
| Maven | 3.9 or later |
| IDE | IntelliJ IDEA recommended |

Contact your assigned technical support representative to obtain the following for the target environment:

- The Station OpenAPI endpoint, for example `http://station.example.com`;
- Either an AppKey and SecretKey pair or an Access Token;
- If RocketMQ messages are required, the NameServer address and confirmation that message consumption is enabled.

The new task template status operation requires platform **v1.3.3 or later**. Task-result MQ messages provide `alarmLevel` from platform v1.3.2; older messages leave it `null`. Confirm support for the features you need with technical support.

Use the complete root endpoint supplied by technical support. It must begin with `http://` or `https://`. Do not append an individual `/remoteApi/*` path.

## Obtain and Integrate the SDK

After cloning or downloading this project from the company's public GitHub repository, choose one of these integration methods:

1. **Copy the source code into your Spring Boot project**. This is the simplest option for direct secondary development.
2. **Publish the SDK to your private Nexus repository**. Build the complete SDK modules, publish them to your corporate Nexus, and then consume them as Maven dependencies.

This project does not provide official JARs through Maven Central. See [Source and Dependency Integration](docs/dependency-and-shade.md) for the exact source directories, dependencies, and private Nexus instructions.

## Quick Evaluation

The repository includes a runnable [Spring Boot Sample](samples/station-openapi-spring-boot-sample/README.md) with:

- All 28 HTTP operations in Swagger UI;
- Token and signature authentication configurations;
- Eleven write or control operations locked by default;
- Streaming file downloads;
- RocketMQ Starter and SDK native subscriber examples.

> [!WARNING]
> **Sample Scope and Liability Notice**
>
> `samples/station-openapi-spring-boot-sample/` exists solely to demonstrate how to configure and call the Station OpenAPI SDK and how to integrate its message support. It is **not a production-ready system**. The Sample does not provide production-grade authentication, authorization, persistence, auditing, rate limiting, monitoring, alerting, high availability, disaster recovery, or comprehensive security controls.
>
> **Do not deploy the Sample application to a real production environment or use it for production workloads before completing the necessary production design, development, security assessment, and comprehensive testing. Any production incident, unintended device operation, service interruption, data loss, corruption or disclosure, credential exposure, or other direct or indirect loss caused by using the Sample directly, or by placing it into production without completing those activities, is solely the user's responsibility. The project provider and maintainers accept no liability.**

For a first evaluation, follow the [Sample README](samples/station-openapi-spring-boot-sample/README.md) to run the application in IDEA. Then follow the [Spring Boot Quickstart](docs/quickstart.md) to integrate the SDK into your own application.

## Documentation

| Goal | Guide |
|---|---|
| Complete the first Spring Boot call | [Spring Boot Quickstart](docs/quickstart.md) |
| Copy source code or use a private Nexus | [Source and Dependency Integration](docs/dependency-and-shade.md) |
| Choose Token or signature authentication | [Authentication and Credentials](docs/auth-and-sign.md) |
| Configure the endpoint, timeouts, and Client | [HTTP and Timeout Configuration](docs/http-client-and-timeout.md) |
| Handle retries and unknown results | [Retries and Result Confirmation](docs/retry-and-idempotency.md) |
| Download map or inspection result files | [File Downloads](docs/file-download-guide.md) |
| Consume the four RocketMQ message types | [RocketMQ Message Integration](docs/mq-events-guide.md) |
| Verify the basic integration | [Integration Checklist](docs/release-checklist.md) |

## Important Usage Rules

- Configure exactly one authentication mode for each `StationOpenApiClient`, and reuse the Client as a singleton Spring bean.
- `secretKey` is used only for local signing and is never sent to the platform. Never store complete credentials in logs, responses, or public source control.
- Device controls, task issuance, and template writes can change platform or field state. Confirm the target and its current state before calling them.
- If a write or control operation fails with `resultUnknown=true`, query the current state or wait for an MQ message before issuing another command.
- Use the streaming download APIs. Do not load a complete file into a `byte[]`.
- Never write camera passwords, raw HTTP bodies, or raw MQ message bodies to logs or external responses.

See [CHANGELOG.md](CHANGELOG.md) for version changes.
