/**
 * OpenAPI 鉴权与签名实现。
 *
 * <p>签名模式在业务 body 副本上追加 {@code appKey/timestamp/nonce/sign}，仅在本地签名副本中加入
 * {@code secretKey}；Token 模式只追加 {@code X-Access-Token} header，并清理 body 中可能误传的签名字段。</p>
 *
 * <p>executor 每次尝试都会重新调用 {@code AuthProvider.authenticate(...)}，因此签名重试会生成新的
 * timestamp、nonce 和 sign。维护此包时不能缓存已经鉴权的请求体，也不能把凭证写入日志或异常。</p>
 */
package com.deeprobotics.station.openapi.sdk.auth;
