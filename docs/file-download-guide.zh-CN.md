# 文件下载

**语言：** [English](file-download-guide.md) | 简体中文

Station OpenAPI 的资源下载接口返回二进制流。传入 `OutputStream` 即可直接接收；SDK 使用 8 KiB 缓冲区，不操作文件系统，也不完整缓存文件。

## 1. 下载到 OutputStream

`filePath` 应来自平台查询结果或 RocketMQ 任务结果，不是客户服务器上的本地路径。

```java
ResourceDownloadRequest request = ResourceDownloadRequest.builder()
        .filePath(platformFilePath)
        .type(ResourceDownloadRequest.Type.RESULT_RESOURCE)
        .build();

client.result().downloadResource(request, outputStream);
```

资源类型：

| 枚举 | 用途 |
|---|---|
| `ResourceDownloadRequest.Type.MAP_FILE` | 地图文件 |
| `ResourceDownloadRequest.Type.RESULT_RESOURCE` | 巡检图片或其他结果资源 |

输出流由调用方持有并关闭。下载只尝试一次，即使设置了 `RetryPolicy.DEFAULT` 也不重试。连接、超时或输出失败时，异常的 `resultUnknown=true` 表示输出可能不完整；重新下载前应丢弃这份输出，不要在其后追加新一次下载的数据。HTTP 错误和异常内容类型会在写出文件数据前拦截，JSON 错误会解析为 SDK 异常。

## 2. 由应用保存文件

如果应用需要本地文件，自行打开文件流即可：

```java
Path target = Path.of("downloads", "inspection-result.jpg");
try (OutputStream output = Files.newOutputStream(target)) {
    client.result().downloadResource(request, output);
}
```

上述代码要求父目录已存在且可写，并会立即创建或清空目标文件。路径、权限、覆盖策略和不完整文件清理由应用负责。转发到其他输出流时，SDK 不需要可写磁盘。大文件不要使用会保存全部内容的 `ByteArrayOutputStream`。

## 3. 通过 Spring MVC 转发

Spring MVC 可以通过 `StreamingResponseBody` 把 SDK 下载流直接写入响应：

```java
StreamingResponseBody body = outputStream ->
        client.result().downloadResource(request, outputStream, options);
```

Servlet 输出流由 Spring 管理，SDK 不会关闭它。数据随接收随转发；如果失败时响应已经提交，就无法改为完整的 ProblemDetail 响应，客户端不能把部分输出当作完整文件。

完整 Spring MVC 示例见：

- [ResultSampleController.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/web/ResultSampleController.java)
- [ResultSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/ResultSampleService.java)

## 4. 大文件超时

大文件可以为单次调用设置更长的 read 和 call timeout：

```java
RequestOptions options = RequestOptions.builder()
        .requestId("result-download-0001")
        .readTimeout(Duration.ofMinutes(2))
        .callTimeout(Duration.ofMinutes(3))
        .build();
```

不要为了一个大文件永久放大所有普通请求的超时。

## 5. 对外提供下载接口

如果客户应用再向其他用户提供下载接口：

- 不要允许外部用户直接提交任意平台 `filePath`；
- 先根据自己的业务资源 ID 完成权限判断，再取得可信的 `filePath`；
- 使用安全文件名，避免路径穿越和响应头注入；
- 下载失败时不要把部分文件标记为成功；
- 不要在日志中输出完整资源地址或原始响应内容。
