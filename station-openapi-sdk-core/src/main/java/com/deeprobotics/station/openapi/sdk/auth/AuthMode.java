package com.deeprobotics.station.openapi.sdk.auth;

/**
 * SDK 鉴权组件使用的模式枚举。
 *
 * <p>平台支持签名模式和 {@code X-Access-Token} 模式，SDK 配置层必须保证二选一。
 * 这里的枚举供 auth provider 和配置桥接使用，不直接承载鉴权凭证。</p>
 */
public enum AuthMode {
    /**
     * {@code appKey + secretKey} 本地签名模式。
     */
    SIGNATURE,
    /**
     * {@code X-Access-Token} 请求头鉴权模式。
     */
    ACCESS_TOKEN
}
