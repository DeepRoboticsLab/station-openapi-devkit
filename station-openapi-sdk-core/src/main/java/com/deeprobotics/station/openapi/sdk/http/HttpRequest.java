package com.deeprobotics.station.openapi.sdk.http;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * transport 可发送的 HTTP 请求快照。
 *
 * <p>该对象只包含 executor 已经完成的结果：最终 URI、header、UTF-8 JSON body 和单次调用超时。
 * transport 不应再修改 body，也不应再次追加签名字段。</p>
 */
public final class HttpRequest {

    /**
     * 最终请求 URI，已经包含 endpoint context path 和平台 API path。
     */
    private final URI url;
    /**
     * 最终请求 header。可能包含鉴权 header，日志输出前必须脱敏。
     */
    private final LinkedHashMap<String, String> headers;
    /**
     * 已序列化完成的请求体字节。普通接口和下载接口都使用 JSON 请求体。
     */
    private final byte[] body;
    /**
     * 本次调用读取超时，可能来自全局配置或 RequestOptions 覆盖。
     */
    private final Duration readTimeout;
    /**
     * 本次调用总超时，可能来自全局配置或 RequestOptions 覆盖。
     */
    private final Duration callTimeout;

    /**
     * 创建不可变 transport 请求快照。
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
        // 防御性复制，避免重试或测试读取时看到调用方后续修改的 body。
        this.body = body == null ? new byte[0] : body.clone();
        this.readTimeout = readTimeout;
        this.callTimeout = callTimeout;
    }

    /**
     * 返回最终请求 URI。
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
        // header 可能包含 token，调用方输出前应自行脱敏；这里仅保证不可修改内部状态。
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
