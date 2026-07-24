package com.deeprobotics.station.openapi.sdk.auth;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.util.Objects;

/**
 * SDK 内部凭证对象。
 *
 * <p>客户通过 {@code StationOpenApiConfig} 或 builder 输入凭证，内部再转换成这个不可变对象。
 * 对象允许读取 secretKey/accessToken 供鉴权实现使用，但字符串化时必须脱敏。</p>
 */
public final class Credential {

    /**
     * 当前凭证所属鉴权模式。它决定后续选择签名 provider 还是 Token provider。
     */
    private final AuthMode authMode;
    /**
     * 签名模式应用标识，会随请求 body 发送给平台。
     */
    private final String appKey;
    /**
     * 签名模式密钥，只在本地参与签名副本计算。
     */
    private final String secretKey;
    /**
     * Token 模式访问令牌，只进入鉴权 header。
     */
    private final String accessToken;

    private Credential(AuthMode authMode, String appKey, String secretKey, String accessToken) {
        this.authMode = Objects.requireNonNull(authMode, "authMode is required");
        this.appKey = appKey;
        this.secretKey = secretKey;
        this.accessToken = accessToken;
    }

    /**
     * 创建签名模式凭证。secretKey 只用于本地签名，不能进入请求体。
     *
     * @param appKey 平台分配的应用标识，不能为 null 或空白
     * @param secretKey 平台分配的签名密钥，不能为 null 或空白
     * @return 不可变的签名模式凭证
     */
    public static Credential signature(String appKey, String secretKey) {
        return new Credential(AuthMode.SIGNATURE, requireText(appKey, "appKey"), requireText(secretKey, "secretKey"),
                null);
    }

    /**
     * 创建 Token 模式凭证。accessToken 只进入 header。
     *
     * @param accessToken 平台分配的访问令牌，不能为 null 或空白
     * @return 不可变的 Token 模式凭证
     */
    public static Credential accessToken(String accessToken) {
        return new Credential(AuthMode.ACCESS_TOKEN, null, null, requireText(accessToken, "accessToken"));
    }

    /**
     * 返回该凭证对应的鉴权模式。
     *
     * @return 签名模式或 Token 模式
     */
    public AuthMode authMode() {
        return authMode;
    }

    /**
     * 返回签名模式 appKey；Token 模式下为 {@code null}。
     *
     * @return appKey，Token 模式下为 {@code null}
     */
    public String appKey() {
        return appKey;
    }

    /**
     * 返回签名模式 secretKey；仅供签名实现使用，调用方不得记录或发送该值。
     *
     * @return secretKey，Token 模式下为 {@code null}
     */
    public String secretKey() {
        return secretKey;
    }

    /**
     * 返回 Token 模式 access token；仅供鉴权 header 使用，调用方不得记录该值。
     *
     * @return access token，签名模式下为 {@code null}
     */
    public String accessToken() {
        return accessToken;
    }

    @Override
    public String toString() {
        // appKey 可以辅助定位租户/应用；secretKey/accessToken 不允许明文输出。
        return "Credential{"
                + "authMode=" + authMode
                + ", appKey='" + appKey + '\''
                + ", secretKey=" + redact(secretKey)
                + ", accessToken=" + redact(accessToken)
                + '}';
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new StationOpenApiClientException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String redact(String value) {
        return value == null ? "null" : "<redacted>";
    }
}
