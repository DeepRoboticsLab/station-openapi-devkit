/**
 * Station OpenAPI SDK 的默认 OkHttp transport 实现。
 *
 * <p>本包把 core 请求快照翻译成一次 OkHttp POST，并把响应转换回 core 类型。默认关闭连接透明重试、
 * HTTP/HTTPS 重定向，并把请求体标记为 one-shot；客户 application interceptor 也被限制为每次 SDK 尝试
 * 只能调用一次 {@code proceed()}，防止绕过 core 的操作意图和重新签名规则。</p>
 *
 * <p>{@code OkHttpTransportFactory} 是本包唯一公开创建入口：默认路径由 Factory 创建并持有
 * OkHttpClient，客户共享 client 路径由 Factory 加固但不接管资源。</p>
 *
 * <p>下载成功只接受 {@code application/octet-stream} 并流式写出；其他 Content-Type 保留有限诊断体交给
 * executor。客户注入的 OkHttpClient 共享连接池、dispatcher、代理和 TLS 设置，生命周期仍由客户管理。</p>
 */
package com.deeprobotics.station.openapi.sdk.transport.okhttp;
