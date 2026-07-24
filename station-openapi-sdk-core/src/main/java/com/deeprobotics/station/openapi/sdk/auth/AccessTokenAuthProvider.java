package com.deeprobotics.station.openapi.sdk.auth;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiAuthException;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code X-Access-Token} 模式鉴权实现。
 *
 * <p>Token 模式只发送该 header，不在请求体中附加签名字段。
 * SDK 仍会主动移除 appKey/timestamp/nonce/sign/secretKey，避免客户误传字段造成排查歧义或泄露风险。</p>
 */
public final class AccessTokenAuthProvider implements AuthProvider {

    /**
     * 平台 Token 模式使用的 header 名称。
     */
    public static final String ACCESS_TOKEN_HEADER = "X-Access-Token";

    /**
     * Token 模式凭证，必须只包含 accessToken。
     */
    private final Credential credential;

    /**
     * 创建 Token 模式 provider。
     *
     * @param credential 必须是 {@link AuthMode#ACCESS_TOKEN} 凭证
     */
    public AccessTokenAuthProvider(Credential credential) {
        this.credential = requireCredential(credential);
    }

    @Override
    public AuthenticatedRequest authenticate(Map<String, Object> businessBody) {
        // Token 模式 body 只保留业务字段；/remoteApi/time 这类无业务字段接口最终会发送 {}。
        LinkedHashMap<String, Object> body = MapUtils.copyWithoutNulls(businessBody);
        removeAuthFields(body);

        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put(ACCESS_TOKEN_HEADER, credential.accessToken());
        return new AuthenticatedRequest(body, headers);
    }

    private static Credential requireCredential(Credential credential) {
        if (credential == null) {
            throw new StationOpenApiAuthException("access token credential is required");
        }
        if (credential.authMode() != AuthMode.ACCESS_TOKEN) {
            throw new StationOpenApiAuthException("access token credential is required");
        }
        return credential;
    }

    private static void removeAuthFields(Map<String, Object> body) {
        // 不让签名字段和 token header 同时出现在一次请求里，保持“二选一”鉴权语义。
        body.remove("appKey");
        body.remove("timestamp");
        body.remove("nonce");
        body.remove("sign");
        body.remove("secretKey");
    }
}
