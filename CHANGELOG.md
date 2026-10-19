# Changelog

**Language:** English | [简体中文](CHANGELOG.zh-CN.md)

## 1.0.0-SNAPSHOT

This is the current GitHub source version. It has not been released as a formal version and is not available as a public Maven Central artifact.

### Breaking changes: direct streaming downloads

- Removes both `ResultApi.downloadResource(request, Path[, options])` overloads. Only the `OutputStream` overloads remain; existing callers must migrate and recompile.
- Streams directly from HTTP to the caller's output with an 8 KiB buffer. The SDK no longer creates temporary files, buffers a complete file, or manages local directories, replacement, and cleanup.
- Downloads make exactly one attempt, including with `RequestOptions.RetryPolicy.DEFAULT`. Connection, timeout, or output failures may leave partial output and report `resultUnknown=true`; discard incomplete output before starting a new download. Ordinary queries retain their existing retry behavior.
- The caller owns and closes the output stream. To save a file, replace the old `Path` call with:

```java
try (OutputStream output = Files.newOutputStream(target)) {
    client.result().downloadResource(request, output, options);
}
```

`Files.newOutputStream` creates or truncates the target immediately and requires an existing parent directory. File permissions, overwrite policy, and partial-file cleanup now belong to the caller. Spring Boot's `StreamingResponseBody` forwards bytes as they arrive; a failure after the HTTP response is committed can no longer become a complete ProblemDetail response. See the [download guide](docs/file-download-guide.md).

### SDK

- Adds nullable string `alarmTime` to robot alarm MQ messages, matching the platform publisher and retaining the original four-argument constructor.
- Aligns signature blank-character handling with the platform, fixing verification failures for ideographic spaces and NBSP without changing business request bodies.
- Omits response bodies from HTTP exceptions and raw Jackson messages/cause chains from JSON exceptions, retaining failure types and request context.
- Hides both passwords and complete stream URLs in camera model `toString()` output; original accessors remain available.
- Adds `taskTemplate().switchStatus(...)` for platform v1.3.3+, bringing HTTP coverage to 28 operations after checking current documentation and backend sources.
- Adds robot posture commands for lying down (15) and standing up (16), retaining raw command codes.
- Adds nullable string `alarmLevel` to task-result MQ messages (platform v1.3.2+), accepting numeric/string values and retaining the original constructor.
- Clarifies Chinese JavaDoc for template status effects, nullable issue results across execution plans, and partial success during batch cancellation.
- Provides seven API groups with 28 Station OpenAPI HTTP operations.
- Supports Access Token and AppKey/SecretKey signature authentication.
- Provides unified pagination, exceptions, request IDs, timeouts, and controlled retries.
- Supports direct streaming downloads through caller-owned `OutputStream` instances.
- Provides optional RocketMQ routing, message models, and a native Subscriber.
- Supports four RocketMQ message types: task status, inspection results, quadruped robot alarms, and inspection routes.
- Provides a normal OkHttp dependency path and a fallback artifact for dependency conflicts.

### Spring Boot Sample

- Both MQ examples display the alarm occurrence time without substituting the current time for missing values.
- Camera views omit stream URLs containing user-info, query parameters, fragments, or invalid syntax to avoid exposing embedded credentials.
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
