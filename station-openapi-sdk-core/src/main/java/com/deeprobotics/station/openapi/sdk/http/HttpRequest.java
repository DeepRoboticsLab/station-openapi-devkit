package com.deeprobotics.station.openapi.sdk.http;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP 传输层使用的不可变请求快照。
 *
 * <p>执行器已完成鉴权和序列化，传输层应原样发送请求头和 UTF-8 JSON 请求体。</p>
 */
public final class HttpRequest {

    private final URI url;
    private final LinkedHashMap<String, String> headers;
    private final byte[] body;
    private final Duration readTimeout;
    private final Duration callTimeout;

    /**
     * 创建请求快照，复制请求头和请求体以隔离调用方的后续修改。
     *
     * @param url 最终请求 URI
     * @param headers 最终 header；null 按空集合处理，null key/value 会被忽略
     * @param body UTF-8 JSON body；null 按空字节数组处理
     * @param readTimeout 本次读取超时；自定义 transport 可为 null 提供自己的兜底值
     * @param callTimeout 本次调用总超时；自定义 transport 可为 null 提供自己的兜底值
     * @throws StationOpenApiClientException header 名称或值包含 CR/LF 时抛出；异常不会回显原值
     */
    public HttpRequest(URI url, Map<String, String> headers, byte[] body, Duration readTimeout,
            Duration callTimeout) {
        this.url = Objects.requireNonNull(url, "url is required");
        this.headers = copyHeaders(headers);
        this.body = body == null ? new byte[0] : body.clone();
        this.readTimeout = readTimeout;
        this.callTimeout = callTimeout;
    }

    /**
     * 返回包含 endpoint 上下文路径和平台接口路径的最终 URI。
     *
     * @return 已解析完成的最终 URI
     */
    public URI url() {
        return url;
    }

    /**
     * 返回 header 副本。内容可能包含 access token，记录日志前必须脱敏。
     *
     * @return 请求头的防御性副本
     */
    public LinkedHashMap<String, String> headers() {
        return new LinkedHashMap<>(headers);
    }

    /**
     * 返回请求体字节副本。
     *
     * @return UTF-8 JSON 请求体的防御性副本
     */
    public byte[] body() {
        return body.clone();
    }

    /**
     * 返回本次请求读取超时。
     *
     * @return 读取超时；自定义请求快照可能为 {@code null}
     */
    public Duration readTimeout() {
        return readTimeout;
    }

    /**
     * 返回本次请求调用总超时。
     *
     * @return 调用总超时；自定义请求快照可能为 {@code null}
     */
    public Duration callTimeout() {
        return callTimeout;
    }

    private static LinkedHashMap<String, String> copyHeaders(Map<String, String> source) {
        LinkedHashMap<String, String> target = new LinkedHashMap<>();
        if (source == null) {
            return target;
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name == null || value == null) {
                continue;
            }
            if (containsCrLf(name)) {
                throw new StationOpenApiClientException("HTTP header name must not contain CR or LF");
            }
            if (containsCrLf(value)) {
                throw new StationOpenApiClientException("HTTP header value must not contain CR or LF");
            }
            target.put(name, value);
        }
        return target;
    }

    private static boolean containsCrLf(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }
}
