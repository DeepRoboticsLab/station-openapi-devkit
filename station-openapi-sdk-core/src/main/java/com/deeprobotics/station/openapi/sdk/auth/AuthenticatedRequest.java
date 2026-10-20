package com.deeprobotics.station.openapi.sdk.auth;

import com.deeprobotics.station.openapi.sdk.exception.Redactor;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 鉴权后的请求快照。
 *
 * <p>签名模式会把 appKey/timestamp/nonce/sign 放入 body；Token 模式会把
 * {@code X-Access-Token} 放入 headers。构造和读取时复制 Map 及请求体的顶层集合容器，不深拷贝嵌套值。</p>
 */
public final class AuthenticatedRequest {

    /**
     * 已补充鉴权信息的请求体。
     *
     * <p>签名模式包含业务字段和 appKey/timestamp/nonce/sign；Token 模式只包含业务字段。</p>
     */
    private final LinkedHashMap<String, Object> body;
    /**
     * 已补充鉴权信息的 header。
     *
     * <p>Token 模式包含 {@code X-Access-Token}；SDK 内置签名模式不发送鉴权 header。</p>
     */
    private final LinkedHashMap<String, String> headers;

    /**
     * 创建鉴权后请求快照，复制 body、顶层集合容器和 header，并省略值为 null 的字段。
     *
     * @param body 已补充鉴权字段的 body；null 按空 body 处理
     * @param headers 鉴权 header；null 按空 header 处理
     */
    public AuthenticatedRequest(Map<String, Object> body, Map<String, String> headers) {
        this.body = MapUtils.copyWithoutNulls(body);
        this.headers = copyHeaders(headers);
    }

    /**
     * 返回请求体 Map 及顶层集合容器的副本，嵌套值仍共享。
     *
     * @return 已鉴权请求体的防御性副本
     */
    public LinkedHashMap<String, Object> body() {
        return MapUtils.copyWithoutNulls(body);
    }

    /**
     * 返回鉴权 header 的副本。内容可能包含 access token，输出前必须脱敏。
     *
     * @return 鉴权请求头的防御性副本
     */
    public LinkedHashMap<String, String> headers() {
        return new LinkedHashMap<>(headers);
    }

    @Override
    public String toString() {
        // body/header 可能包含 sign 或 token，统一走 Redactor 后再输出。
        return Redactor.redact("AuthenticatedRequest{"
                + "body=" + body
                + ", headers=" + headers
                + '}');
    }

    private static LinkedHashMap<String, String> copyHeaders(Map<String, String> source) {
        LinkedHashMap<String, String> target = new LinkedHashMap<>();
        if (source == null) {
            return target;
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (entry.getValue() != null) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
        return target;
    }
}
