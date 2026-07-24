# Source and Dependency Integration

**Language:** English | [简体中文](dependency-and-shade.zh-CN.md)

Station OpenAPI SDK is distributed as source code through the company's public GitHub repository. No official JAR is published to Maven Central. The current source and private artifact version is `1.0.0-SNAPSHOT`.

You can either copy the source code directly or publish the SDK to your own private Nexus repository.

## 1. Copy the Source Code

Merge the following source and resource content into the corresponding directories of your Spring Boot module while preserving the original packages:

| Content in this repository | Destination in the customer project |
|---|---|
| Everything under `station-openapi-sdk-core/src/main/java/` | `src/main/java/` |
| Everything under `station-openapi-sdk-transport-okhttp/src/main/java/` | `src/main/java/` |
| Everything under `station-openapi-sdk-transport-okhttp/src/main/resources/` | `src/main/resources/` |

The transport resources contain the JDK SPI registration for the default HTTP transport and must be copied:

```text
META-INF/services/com.deeprobotics.station.openapi.sdk.http.HttpTransportProvider
```

If this file is missing, `StationOpenApiClients.builder().build()` cannot discover the default OkHttp implementation.

Projects using `spring-boot-starter-web` normally already include Jackson and SLF4J. Also confirm that the project includes OkHttp:

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

The root `pom.xml` is authoritative for exact dependency versions. Copy all SDK source content; do not omit `internal` packages, because the public APIs depend on those execution implementations.

### Add RocketMQ Support

To use the SDK message models, Router, or native Subscriber, also copy:

```text
station-openapi-sdk-rocketmq/src/main/java/
```

For Spring Boot, use the [main Sample POM](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/pom.xml) as the reference dependency combination for SDK RocketMQ source and RocketMQ Starter. See [RocketMQ Message Integration](mq-events-guide.md) for configuration.

## 2. Publish to a Private Nexus Repository

If multiple applications need the SDK, the customer team can build it and publish it to the corporate Nexus repository.

Build the root project with JDK 17 and Maven 3.9+, then use the organization's existing Maven and Nexus publishing configuration to publish the complete SDK module set. Do not upload only the `station-openapi-sdk` JAR, because it depends on core, the OkHttp transport, and their POM metadata.

After publishing, add this dependency to a Spring Boot project:

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Add the optional RocketMQ module only when needed:

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk-rocketmq</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The business project must declare its corporate Nexus address and authentication through its own Maven configuration. The SDK repository does not provide or modify customer Nexus settings.

## 3. Default Dependencies and Conflict Handling

`station-openapi-sdk` is the default aggregate for normal HTTP integration. It includes:

- `station-openapi-sdk-core`: public APIs, request and response models, authentication, exceptions, and retries;
- `station-openapi-sdk-transport-okhttp`: the default HTTP implementation;
- OkHttp and its transitive dependencies.

If the application already uses other OkHttp, Okio, or Kotlin versions, inspect the final versions in IDEA's Maven dependency view and align them. Do not retain multiple versions, and do not change OkHttp to `provided`.

Use `station-openapi-sdk-all` only when normal dependency alignment cannot resolve a conflict:

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk-all</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Choose either `station-openapi-sdk-all` or the default `station-openapi-sdk`, never both. The all-in-one artifact is a conflict fallback, not the default choice.

## 4. Integration Checks

- The application uses JDK 17 or later.
- Source integration includes the SPI resource file.
- The private Nexus contains every required SDK POM and JAR.
- `StationOpenApiClients.builder().build()` creates the Client successfully.
- The first `client.system().getTimeText()` call succeeds.
