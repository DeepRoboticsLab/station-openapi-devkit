package com.deeprobotics.station.openapi.sdk.auth;

import com.deeprobotics.station.openapi.sdk.exception.Redactor;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 鉴权后的请求快照。
 *
 * <p>签名模式会把 appKey/timestamp/nonce/sign 放入 body；Token 模式会把
 * {@code X-Access-Token} 放入 headers。对象构造时会防御性拷贝，避免调用方后续修改影响已准备发送的请求。</p>
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
     * 创建鉴权后请求快照，并对 body 中的集合和 header 做防御性复制。
     *
     * @param body 已补充鉴权字段的 body；null 按空 body 处理
     * @param headers 鉴权 header；null 按空 header 处理
     */
    public AuthenticatedRequest(Map<String, Object> body, Map<String, String> headers) {
        this.body = MapUtils.copyWithoutNulls(body);
        this.headers = copyHeaders(headers);
    }

    /**
     * 返回可序列化 body 的副本。调用方修改返回值不会改变内部快照。
     *
     * @return 已鉴权请求体的防御性副本
     */
    public LinkedHashMap<String, Object> body() {
        // 返回副本，避免调用者修改已鉴权 body 后造成签名和最终 JSON 不一致。
        return MapUtils.copyWithoutNulls(body);
    }

    /**
     * 返回鉴权 header 的副本。内容可能包含 access token，输出前必须脱敏。
     *
     * @return 鉴权请求头的防御性副本
     */
    public LinkedHashMap<String, String> headers() {
        // header 可能包含凭证，返回副本只保证不可变性；输出日志仍必须走 Redactor。
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
