/**
 * 与具体 HTTP 客户端无关的 transport 契约、请求/响应快照和 endpoint 元数据。
 *
 * <p>{@code sdk-core} 只能依赖本包抽象，不能引用 {@code okhttp3.*}。transport 负责一次底层 I/O 和网络异常分类；
 * 鉴权、业务重试、{@code R<T>} 解包、下载协议判断和客户异常转换仍由 core executor 负责。</p>
 *
 * <p>{@code HttpTransportProvider} 是默认 transport 的 JDK SPI 扩展点。provider 只能接收建连超时等非敏感设置，
 * 每次创建独立 transport，不能接触 endpoint 或客户凭证。</p>
 */
package com.deeprobotics.station.openapi.sdk.http;
