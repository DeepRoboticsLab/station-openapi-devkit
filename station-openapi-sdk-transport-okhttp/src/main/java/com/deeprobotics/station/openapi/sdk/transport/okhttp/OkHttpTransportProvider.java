package com.deeprobotics.station.openapi.sdk.transport.okhttp;

import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportProvider;
import com.deeprobotics.station.openapi.sdk.http.HttpTransportSettings;
import java.util.Objects;

/**
 * Station OpenAPI SDK 的默认 OkHttp transport provider。
 *
 * <p>该类由 JDK {@link java.util.ServiceLoader} 自动发现。每次调用都创建独立、由 SDK 客户端持有并关闭的
 * transport，不缓存客户端、连接池或任何鉴权信息。</p>
 */
public final class OkHttpTransportProvider implements HttpTransportProvider {

    /**
     * JDK ServiceLoader 使用的公开无参构造器。
     */
    public OkHttpTransportProvider() {
    }

    @Override
    public HttpTransport create(HttpTransportSettings settings) {
        HttpTransportSettings required = Objects.requireNonNull(settings, "settings is required");
        return OkHttpTransportFactory.create(required.connectTimeout());
    }
}
