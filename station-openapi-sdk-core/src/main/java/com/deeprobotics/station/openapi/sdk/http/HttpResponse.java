package com.deeprobotics.station.openapi.sdk.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * transport 返回给 executor 的 HTTP 响应快照。
 *
 * <p>普通 2xx JSON 请求 body 是完整响应体；普通非 2xx 以及下载请求的错误或协议 body 应由 transport
 * 限制为有界诊断体。二进制成功内容应已经写入下载输出流。</p>
 */
public final class HttpResponse {

    /**
     * HTTP 状态码。业务成功还需要 executor 继续解包 {@code R<T>}。
     */
    private final int statusCode;
    /**
     * 响应 header。下载响应需要通过 Content-Type 判断二进制成功还是 JSON 错误。
     */
    private final LinkedHashMap<String, List<String>> headers;
    /**
     * 响应体快照。普通 2xx JSON 是完整 body；二进制下载成功时为空，非 2xx 或协议异常时为有界诊断体。
     */
    private final byte[] body;

    /**
     * 创建不可变 transport 响应快照。
     *
     * @param statusCode HTTP 状态码
     * @param headers 多值响应 header；null 按空集合处理
     * @param body 普通 2xx 请求的完整 body，或非 2xx、下载失败、协议异常的有界诊断 body
     */
    public HttpResponse(int statusCode, Map<String, List<String>> headers, byte[] body) {
        this.statusCode = statusCode;
        this.headers = copyHeaders(headers);
        // 响应体可能会进入异常片段，executor 会截断并脱敏；这里先保证不可变。
        this.body = body == null ? new byte[0] : body.clone();
    }

    /**
     * 返回 HTTP 状态码。
     *
     * @return HTTP status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * 返回 header 及其多值列表的防御性副本。
     *
     * @return 响应头的深一层防御性副本
     */
    public LinkedHashMap<String, List<String>> headers() {
        return copyHeaders(headers);
    }

    /**
     * 返回响应体字节副本。
     *
     * @return 响应体的防御性副本
     */
    public byte[] body() {
        return body.clone();
    }

    private static LinkedHashMap<String, List<String>> copyHeaders(Map<String, List<String>> source) {
        LinkedHashMap<String, List<String>> target = new LinkedHashMap<>();
        if (source == null) {
            return target;
        }
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                target.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        return target;
    }
}
