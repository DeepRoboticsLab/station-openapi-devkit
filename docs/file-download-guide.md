# File Downloads

**Language:** English | [简体中文](file-download-guide.zh-CN.md)

The Station OpenAPI resource download operation returns a binary stream. Pass an `OutputStream` to receive it directly.
The SDK uses an 8 KiB buffer, performs no filesystem operations, and never buffers the complete file.

## 1. Download to an OutputStream

`filePath` must come from a platform query result or a RocketMQ task result. It is not a path on the customer
application's local filesystem.

```java
ResourceDownloadRequest request = ResourceDownloadRequest.builder()
        .filePath(platformFilePath)
        .type(ResourceDownloadRequest.Type.RESULT_RESOURCE)
        .build();

client.result().downloadResource(request, outputStream);
```

Resource types:

| Enum                                           | Purpose                                     |
|------------------------------------------------|---------------------------------------------|
| `ResourceDownloadRequest.Type.MAP_FILE`        | Map file                                    |
| `ResourceDownloadRequest.Type.RESULT_RESOURCE` | Inspection image or another result resource |

The caller owns and closes the output stream. Downloads make one attempt, regardless of retry settings. Connection
failures, timeouts, and output failures report `resultUnknown=true`; discard incomplete output before restarting. Only
successful `application/octet-stream` responses are written; JSON errors are parsed into SDK exceptions.

## 2. Save a File in Your Application

If your application needs a local file, open its stream yourself:

```java
Path target = Path.of("downloads", "inspection-result.jpg");
try (OutputStream output = Files.newOutputStream(target)) {
    client.result().downloadResource(request, output);
}
```

The parent directory must exist and be writable. `Files.newOutputStream` creates or truncates the target; your
application handles incomplete-file cleanup. Avoid `ByteArrayOutputStream` for large files because it holds the entire
file in memory.

## 3. Forward Through Spring MVC

Spring MVC can use `StreamingResponseBody` to write the SDK stream directly to the HTTP response:

```java
StreamingResponseBody body = outputStream ->
        client.result().downloadResource(request, outputStream, options);
```

Spring owns the Servlet output stream; the SDK never closes it. Bytes are forwarded as they arrive. If the response has
already been committed when a failure occurs, it cannot be replaced with a complete ProblemDetail response; the client
must not treat partial output as a complete file.

`StreamingResponseBody` starts MVC asynchronous processing. The Sample sets `spring.mvc.async.request-timeout=0` to
disable its total timeout. This setting applies to all MVC asynchronous requests and can be overridden with a positive
duration; SDK connection and read timeouts still apply.

See these complete Spring MVC examples:

- [ResultSampleController.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/web/ResultSampleController.java)
- [ResultSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/ResultSampleService.java)

## 4. Download Timeouts

Downloads have no total timeout by default and do not inherit the Client's global call timeout. Connection and read
timeouts still apply: 3 seconds to connect and 10 seconds per blocking read by default.

Use `RequestOptions.readTimeout` for longer waits for data, or `RequestOptions.callTimeout` to set a total limit for one
download. See [HTTP and Timeout Configuration](http-client-and-timeout.md) for an example.

## 5. Expose Downloads Through Your Application

If the customer application exposes a download API to other users:

- Authorize access using a business resource ID, then obtain a trusted `filePath`; do not accept arbitrary platform
  paths;
- Use a safe filename to prevent path traversal and response header injection;
- Do not report success when only a partial file was produced;
- Do not log complete resource locations or raw response content.
