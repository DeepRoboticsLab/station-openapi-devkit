# 文件下载

**语言：** [English](file-download-guide.md) | 简体中文

Station OpenAPI 的资源下载接口返回二进制流，不是普通 JSON 响应。SDK 提供保存到 `Path` 和写入 `OutputStream` 两种方式。

## 1. 选择下载方式

| 场景 | 使用方式 |
|---|---|
| 保存到本地磁盘或挂载目录 | `Path` |
| 通过 Spring MVC 返回给浏览器 | `OutputStream` |
| 转发到自己的流式存储 | `OutputStream` |

不要先把完整文件读取到 `byte[]`，否则大文件会占用大量堆内存。

## 2. 保存到 Path

`filePath` 应来自平台查询结果或 RocketMQ 任务结果，不是客户服务器上的本地路径。

```java
ResourceDownloadRequest request = ResourceDownloadRequest.builder()
        .filePath(platformFilePath)
        .type(ResourceDownloadRequest.Type.RESULT_RESOURCE)
        .build();

Path target = Path.of("downloads", "inspection-result.jpg");
Path saved = client.result().downloadResource(request, target);
```

资源类型：

| 枚举 | 用途 |
|---|---|
| `ResourceDownloadRequest.Type.MAP_FILE` | 地图文件 |
| `ResourceDownloadRequest.Type.RESULT_RESOURCE` | 巡检图片或其他结果资源 |

SDK 会使用临时文件隔离失败尝试，下载完整后再生成目标文件。

## 3. 写入 OutputStream

Spring MVC 可以通过 `StreamingResponseBody` 把 SDK 下载流直接写入响应：

```java
StreamingResponseBody body = outputStream ->
        client.result().downloadResource(request, outputStream, options);
```

调用方负责创建和关闭 `OutputStream`；SDK 不会关闭它。连接中断时输出可能只包含部分字节，调用方不能把这部分内容当作完整文件。

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
