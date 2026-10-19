package com.deeprobotics.station.openapi.sdk.http;

import java.io.OutputStream;

/**
 * SDK core 与具体 HTTP 客户端之间的抽象边界。
 *
 * <p>core 只能依赖这个接口，不能依赖具体 HTTP 客户端或客户项目中的任何 HTTP 实现。
 * 默认 transport 必须实现这里的契约。</p>
 */
public interface HttpTransport extends AutoCloseable {

    /**
     * 发送 JSON POST 请求并返回响应快照。
     *
     * <p>transport 不负责解包 {@code R<T>}，也不决定业务重试；这些都由 {@code OpenApiExecutor} 统一处理。
     * 普通 2xx JSON body 必须完整保留，供 executor 解析业务数据；普通非 2xx body 应由实现设置安全的
     * 诊断长度上限，避免网关错误页或异常响应造成无界内存占用。
     * 网络超时应抛 {@link HttpTimeoutException}，DNS、TLS、断连等连接失败应抛
     * {@link HttpConnectionException}。其他运行时异常或 null 响应会被 executor 保守包装为统一
     * transport 异常且不会自动重试。</p>
     *
     * @param request executor 已经完成鉴权和 JSON 序列化的请求快照
     * @return HTTP status、header，以及完整 2xx body 或有界非 2xx 诊断 body；实现不得返回 {@code null}
     */
    HttpResponse postJson(HttpRequest request);

    /**
     * 执行文件下载。
     *
     * <p>二进制成功响应应直接分块写入 {@code outputStream}，不暂存、不重试、不完整缓存文件。
     * 如果远端返回非 2xx，或 2xx 响应不是 {@code application/octet-stream}，transport 需要让 executor
     * 能从 {@link HttpResponse#body()} 读取有限诊断片段；不要把 JSON、HTML 或未知类型写进客户文件。
     * 网络异常分类与 {@link #postJson(HttpRequest)} 相同；输出流写入失败应抛出
     * {@link com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException
     * StationOpenApiDownloadException}，并设置 {@code resultUnknown=true}。</p>
     *
     * @param request executor 已经完成鉴权和 JSON 序列化的下载请求快照
     * @param outputStream 二进制成功响应的写入目标；transport 不负责关闭
     * @return HTTP status、header，以及非二进制响应的有限诊断 body；实现不得返回 {@code null}
     */
    HttpResponse download(HttpRequest request, OutputStream outputStream);

    /**
     * 释放 transport 持有的底层资源。
     *
     * <p>通过默认 provider 创建的 transport 由 SDK 客户端关闭；通过
     * {@code StationOpenApiClients.Builder.transport(...)} 显式注入的 transport 始终由客户关闭，
     * SDK 客户端不会接管客户共享连接资源的生命周期。</p>
     */
    @Override
    void close();
}
