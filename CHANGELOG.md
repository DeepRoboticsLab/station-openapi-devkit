# Changelog

**Language:** English | [简体中文](CHANGELOG.zh-CN.md)

## v1.1.0

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

`Files.newOutputStream` creates or truncates the target and requires an existing parent directory. The application handles incomplete-file cleanup. See the [download guide](docs/file-download-guide.md).

### SDK

- Removes the default total timeout from downloads so progressing transfers can outlast the global JSON call timeout. Connection and read timeouts remain active; callers that need a download total limit must set a positive `RequestOptions.callTimeout` explicitly. Custom transports must interpret a zero `HttpRequest.callTimeout()` as no total timeout.
- Aligns direct OkHttp download defaults with the public SDK: a 10-second read wait and no total timeout, replacing the separate 60-second read fallback. Removes Dispatcher concurrency settings that do not limit synchronous calls.
- Adds nullable string `alarmTime` to robot alarm MQ messages (platform v1.3.3+), retaining the original four-argument constructor.
- Aligns signature blank-character handling with the platform, fixing verification failures for ideographic spaces and NBSP without changing business request bodies.
- Omits response bodies from HTTP exceptions and raw Jackson messages/cause chains from JSON exceptions, retaining failure types and request context.
- Hides both passwords and complete stream URLs in camera model `toString()` output; original accessors remain available.
- Adds `taskTemplate().switchStatus(...)` for platform v1.3.3+, bringing HTTP coverage to 28 operations. Disabling clears pending tasks; enabling requires another issue call.
- Adds robot posture commands for lying down (15) and standing up (16), retaining raw command codes.
- Adds nullable string `alarmLevel` to task-result MQ messages (platform v1.3.2+), accepting numeric/string values and retaining the original constructor.

### Spring Boot Sample

- Sets `spring.mvc.async.request-timeout=0` to disable the MVC asynchronous timeout used by `StreamingResponseBody`. Applications can override it; a failure after response commitment may leave partial output and cannot become a complete ProblemDetail response.
- Both MQ examples display the alarm occurrence time without substituting the current time for missing values.
- Camera views omit stream URLs containing user-info, query parameters, fragments, or invalid syntax to avoid exposing embedded credentials.
- Requires a JSON integer for template status, preventing fractional values from being truncated into control commands; JSON parsing errors return localized ProblemDetail without echoing input.
- Adds template status Swagger documentation, SDK calls, bilingual validation, and default protection, bringing protected operations to 11; both MQ examples display the alarm level.

### Documentation

- Updates the download, timeout, MQ, and Maven integration guides and aligns the English and Chinese examples.
- Clarifies JavaDoc for template status effects, nullable task issue results, and partial success during batch cancellation.

## 1.0.0-SNAPSHOT

Initial source version, with no public Maven Central artifact.

### SDK

- Provides seven API groups with 27 Station OpenAPI HTTP operations.
- Supports Access Token and AppKey/SecretKey signature authentication.
- Provides unified pagination, exceptions, request IDs, timeouts, and controlled retries.
- Supports streaming downloads through `Path` and `OutputStream`.
- Provides optional RocketMQ routing, message models, and a native Subscriber.
- Supports four RocketMQ message types: task status, inspection results, quadruped robot alarms, and inspection routes.
- Provides a normal OkHttp dependency path and a fallback artifact for dependency conflicts.

### Spring Boot Sample

- Uses Spring Boot 3.5.16 and JDK 17.
- Exposes all 27 SDK HTTP calls through Swagger UI.
- Supports Token and signature configurations.
- Locks ten write or control operations by default.
- Provides Chinese and English validation and SDK error responses.
- Streams files through `StreamingResponseBody`.
- Includes a RocketMQ Starter example and a supplemental native Subscriber example.

### Distribution

The project is distributed as source code through the company's public GitHub repository. Customers can:

- Copy SDK source code into a Spring Boot project;
- Build the complete SDK modules and publish them to a private corporate Nexus repository.

See [Source and Dependency Integration](docs/dependency-and-shade.md) for integration and the [Sample README](samples/station-openapi-spring-boot-sample/README.md) for evaluation instructions.
