# File Downloads

**Language:** English | [简体中文](file-download-guide.zh-CN.md)

The Station OpenAPI resource download operation returns a binary stream. Pass an `OutputStream` to receive it directly. The SDK uses an 8 KiB buffer, performs no filesystem operations, and never buffers the complete file.

## 1. Download to an OutputStream

`filePath` must come from a platform query result or a RocketMQ task result. It is not a path on the customer application's local filesystem.

```java
ResourceDownloadRequest request = ResourceDownloadRequest.builder()
        .filePath(platformFilePath)
        .type(ResourceDownloadRequest.Type.RESULT_RESOURCE)
        .build();

client.result().downloadResource(request, outputStream);
```

Resource types:

| Enum | Purpose |
|---|---|
| `ResourceDownloadRequest.Type.MAP_FILE` | Map file |
| `ResourceDownloadRequest.Type.RESULT_RESOURCE` | Inspection image or another result resource |

The caller owns and closes the output stream. The SDK makes one attempt, even with `RetryPolicy.DEFAULT`. Connection failures, timeouts, and output failures report `resultUnknown=true`: output may be incomplete. Discard that output before restarting; never append a new attempt to it. HTTP errors and unexpected content types are checked before any file bytes are written, and JSON errors are parsed into SDK exceptions.

## 2. Save a File in Your Application

If your application needs a local file, open its stream yourself:

```java
Path target = Path.of("downloads", "inspection-result.jpg");
try (OutputStream output = Files.newOutputStream(target)) {
    client.result().downloadResource(request, output);
}
```

This requires an existing, writable parent directory and immediately creates or truncates the target. Your application manages paths, permissions, overwrite policy, and incomplete-file cleanup. The SDK does not require a writable disk when forwarding to another stream. Avoid using a `ByteArrayOutputStream` for large files because it holds the entire file in memory.

## 3. Forward Through Spring MVC

Spring MVC can use `StreamingResponseBody` to write the SDK stream directly to the HTTP response:

```java
StreamingResponseBody body = outputStream ->
        client.result().downloadResource(request, outputStream, options);
```

Spring owns the Servlet output stream; the SDK never closes it. Bytes are forwarded as they arrive. If the response has already been committed when a failure occurs, it cannot be replaced with a complete ProblemDetail response; the client must not treat partial output as a complete file.

See these complete Spring MVC examples:

- [ResultSampleController.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/web/ResultSampleController.java)
- [ResultSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/ResultSampleService.java)

## 4. Timeouts for Large Files

Use longer read and call timeouts for one large download when necessary:

```java
RequestOptions options = RequestOptions.builder()
        .requestId("result-download-0001")
        .readTimeout(Duration.ofMinutes(2))
        .callTimeout(Duration.ofMinutes(3))
        .build();
```

Do not permanently increase every normal request timeout to accommodate one large file.

## 5. Expose Downloads Through Your Application

If the customer application exposes a download API to other users:

- Do not allow an external caller to submit an arbitrary platform `filePath`;
- Authorize access using your own business resource ID, then obtain a trusted `filePath`;
- Use a safe filename to prevent path traversal and response header injection;
- Do not report success when only a partial file was produced;
- Do not log complete resource locations or raw response content.
