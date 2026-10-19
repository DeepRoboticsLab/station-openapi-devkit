package com.deeprobotics.station.openapi.sdk.auth;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiAuthException;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code X-Access-Token} 模式鉴权实现。
 *
 * <p>仅通过令牌头鉴权，并从业务请求体移除 appKey/timestamp/nonce/sign/secretKey。</p>
 */
public final class AccessTokenAuthProvider implements AuthProvider {

    /**
     * 平台 Token 模式使用的 header 名称。
     */
    public static final String ACCESS_TOKEN_HEADER = "X-Access-Token";

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
        body.remove("appKey");
        body.remove("timestamp");
        body.remove("nonce");
        body.remove("sign");
        body.remove("secretKey");
    }
}
