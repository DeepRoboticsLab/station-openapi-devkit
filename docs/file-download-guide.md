# File Downloads

**Language:** English | [简体中文](file-download-guide.zh-CN.md)

The Station OpenAPI resource download operation returns a binary stream, not a normal JSON response. The SDK can save the result to a `Path` or write it to an `OutputStream`.

## 1. Choose a Download Method

| Scenario | Method |
|---|---|
| Save to a local disk or mounted directory | `Path` |
| Return the file to a browser through Spring MVC | `OutputStream` |
| Forward the file to your own streaming storage | `OutputStream` |

Do not read the complete file into a `byte[]`, because large files can consume substantial heap memory.

## 2. Save to a Path

`filePath` must come from a platform query result or a RocketMQ task result. It is not a path on the customer application's local filesystem.

```java
ResourceDownloadRequest request = ResourceDownloadRequest.builder()
        .filePath(platformFilePath)
        .type(ResourceDownloadRequest.Type.RESULT_RESOURCE)
        .build();

Path target = Path.of("downloads", "inspection-result.jpg");
Path saved = client.result().downloadResource(request, target);
```

Resource types:

| Enum | Purpose |
|---|---|
| `ResourceDownloadRequest.Type.MAP_FILE` | Map file |
| `ResourceDownloadRequest.Type.RESULT_RESOURCE` | Inspection image or another result resource |

The SDK isolates failed attempts with a temporary file and creates the target file only after a complete download.

## 3. Write to an OutputStream

Spring MVC can use `StreamingResponseBody` to write the SDK stream directly to the HTTP response:

```java
StreamingResponseBody body = outputStream ->
        client.result().downloadResource(request, outputStream, options);
```

The caller creates and closes the `OutputStream`; the SDK never closes it. If the connection is interrupted, the output may contain only partial bytes and must not be treated as a complete file.

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
