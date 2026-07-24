package com.deeprobotics.station.openapi.sdk.http;

/**
 * 默认 HTTP transport 的 JDK SPI 扩展点。
 *
 * <p>普通客户引入的 transport 模块通过 {@code META-INF/services} 注册实现。provider 只能接收
 * {@link HttpTransportSettings} 中的非敏感网络配置，不能接触 accessToken 或 secretKey。</p>
 */
public interface HttpTransportProvider {

    /**
     * 创建一个全新且由 SDK 客户端持有生命周期的 transport。
     *
     * <p>实现不得返回共享单例；一个客户端关闭时不能影响其他客户端。</p>
     *
     * @param settings 不包含 endpoint 或凭证的网络设置
     * @return 新创建且由 SDK 关闭的 transport，不能为 {@code null}
     */
    HttpTransport create(HttpTransportSettings settings);
}
