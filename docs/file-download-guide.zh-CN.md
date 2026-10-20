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

| 枚举                                           | 用途                   |
|------------------------------------------------|------------------------|
| `ResourceDownloadRequest.Type.MAP_FILE`        | 地图文件               |
| `ResourceDownloadRequest.Type.RESULT_RESOURCE` | 巡检图片或其他结果资源 |

输出流由调用方持有并关闭。下载只尝试一次，不受重试配置影响。连接、超时或输出失败时标记 `resultUnknown=true`
，重新下载前须丢弃不完整输出。仅成功的 `application/octet-stream` 响应会写入输出流；JSON 错误会解析为 SDK 异常。

## 2. 由应用保存文件

如果应用需要本地文件，自行打开文件流即可：

```java
Path target = Path.of("downloads", "inspection-result.jpg");
try (OutputStream output = Files.newOutputStream(target)) {
    client.result().downloadResource(request, output);
}
```

父目录必须已存在且可写。`Files.newOutputStream` 会创建或清空目标文件，不完整文件由应用清理。大文件不要使用会保存全部内容的
`ByteArrayOutputStream`。

## 3. 通过 Spring MVC 转发

Spring MVC 可以通过 `StreamingResponseBody` 把 SDK 下载流直接写入响应：

```java
StreamingResponseBody body = outputStream ->
        client.result().downloadResource(request, outputStream, options);
```

Servlet 输出流由 Spring 管理，SDK 不会关闭它。数据随接收随转发；如果失败时响应已经提交，就无法改为完整的 ProblemDetail
响应，客户端不能把部分输出当作完整文件。

`StreamingResponseBody` 会启动 MVC 异步处理。Sample 设置 `spring.mvc.async.request-timeout=0`，关闭其总超时。该配置作用于所有
MVC 异步请求，可覆盖为正数时长；SDK 的连接和读取超时仍然生效。

完整 Spring MVC 示例见：

- [ResultSampleController.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/web/ResultSampleController.java)
- [ResultSampleService.java](../samples/station-openapi-spring-boot-sample/station-openapi-sample-spring-boot/src/main/java/com/deeprobotics/station/openapi/sample/service/ResultSampleService.java)

## 4. 下载超时

下载默认没有总时限，不继承 Client 的全局 call timeout。连接和读取超时仍生效：默认连接超时 3 秒，每次阻塞读取最多等待 10 秒。

需要延长数据等待时间时设置 `RequestOptions.readTimeout`；需要限制单次下载总时长时设置 `RequestOptions.callTimeout`
。示例见 [HTTP 与超时配置](http-client-and-timeout.zh-CN.md)。

## 5. 对外提供下载接口

如果客户应用再向其他用户提供下载接口：

- 根据业务资源 ID 校验权限后取得可信的 `filePath`，不要接受外部用户提交的任意平台路径；
- 使用安全文件名，避免路径穿越和响应头注入；
- 下载失败时不要把部分文件标记为成功；
- 不要在日志中输出完整资源地址或原始响应内容。
