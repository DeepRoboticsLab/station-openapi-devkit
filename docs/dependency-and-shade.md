# Source and Dependency Integration

**Language:** English | [简体中文](dependency-and-shade.zh-CN.md)

Station OpenAPI SDK is distributed as source code through GitHub. No official JAR is published to Maven Central. The
current source version is `1.1.0` and requires JDK 17 or later.

Copy the source code, install the SDK in your local Maven repository, or publish it to a private Nexus repository.

## 1. Copy the Source Code

Merge the following source and resource content into the corresponding directories of your Spring Boot module while
preserving the original packages:

| Content in this repository                                                  | Destination in the customer project |
|-----------------------------------------------------------------------------|-------------------------------------|
| Everything under `station-openapi-sdk-core/src/main/java/`                  | `src/main/java/`                    |
| Everything under `station-openapi-sdk-transport-okhttp/src/main/java/`      | `src/main/java/`                    |
| Everything under `station-openapi-sdk-transport-okhttp/src/main/resources/` | `src/main/resources/`               |

The transport resources contain the JDK SPI registration for the default HTTP transport and must be copied:

```text
META-INF/services/com.deeprobotics.station.openapi.sdk.http.HttpTransportProvider
```

Without this file, the SDK cannot discover its default OkHttp transport.

Projects using `spring-boot-starter-web` normally already include Jackson and SLF4J. Also confirm that the project
includes OkHttp:

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

Use the dependency versions in the [root POM](../pom.xml). Copy the complete source directories, including `internal`
packages.

### Add RocketMQ Support

To use the SDK message models, Router, or native Subscriber, also copy:

```text
station-openapi-sdk-rocketmq/src/main/java/
```

Include the native client dependency declared in the [RocketMQ module POM](../station-openapi-sdk-rocketmq/pom.xml). For
RocketMQ Starter dependencies and configuration, see [RocketMQ Message Integration](mq-events-guide.md).

## 2. Use a Maven Repository

For local development, run this from the repository root with JDK 17 and Maven 3.9+:

```shell
mvn clean install
```

For shared use, publish the parent POM and SDK modules through your organization's Maven/Nexus configuration. The
`station-openapi-sdk` JAR alone is insufficient: it depends on core, the OkHttp transport, and their POMs.

Once installed or published, add:

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk</artifactId>
    <version>1.1.0</version>
</dependency>
```

Add the optional RocketMQ module only when needed:

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk-rocketmq</artifactId>
    <version>1.1.0</version>
</dependency>
```

When using Nexus, configure its address and authentication in your application's Maven environment.

## 3. Default Dependencies and Conflict Handling

`station-openapi-sdk` is the default aggregate for normal HTTP integration. It includes:

- `station-openapi-sdk-core`: public APIs, request and response models, authentication, exceptions, and retries;
- `station-openapi-sdk-transport-okhttp`: the default HTTP implementation;
- OkHttp and its transitive dependencies.

If the application already uses other OkHttp, Okio, or Kotlin versions, inspect the final versions in IDEA's Maven
dependency view and align them. Do not retain multiple versions, and do not change OkHttp to `provided`.

Use `station-openapi-sdk-all` only when normal dependency alignment cannot resolve a conflict:

```xml
<dependency>
    <groupId>com.deeprobotics.station.openapi</groupId>
    <artifactId>station-openapi-sdk-all</artifactId>
    <version>1.1.0</version>
</dependency>
```

Choose either `station-openapi-sdk-all` or `station-openapi-sdk`. The shaded artifact includes the HTTP SDK and
relocates `okhttp3`, `okio`, and `kotlin`; it does not include RocketMQ. Jackson and SLF4J retain their original package
names.

Continue with the [Spring Boot Quickstart](quickstart.md) to configure the Client and make the first call.
