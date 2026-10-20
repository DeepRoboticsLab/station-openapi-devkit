package com.deeprobotics.station.openapi.sdk.transport.okhttp;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

/**
 * OkHttp transport 工厂和 SDK 默认客户端配置入口。
 *
 * <p>客户如果没有特殊代理、证书、连接池或监控拦截器需求，直接使用 {@link #create()} 即可。
 * 需要接入企业统一网络栈时，可以自行构造 OkHttpClient 后调用 {@link #create(OkHttpClient)}；
 * SDK 会派生共享底层资源的 client，并强制保留禁止透明重试和重定向的安全策略。</p>
 */
public final class OkHttpTransportFactory {

    /** 建连超时默认 3 秒，快速暴露地址、DNS 或网络不可达问题。 */
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    /** 普通请求读超时默认 10 秒；单次请求可通过 RequestOptions 覆盖。 */
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);
    /** 普通 JSON 调用的默认总时限；transport 为下载单独关闭总时限。 */
    private static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(15);
    /** 默认保留少量空闲连接，兼顾 SDK 连续调用性能和客户进程资源占用。 */
    private static final int DEFAULT_MAX_IDLE_CONNECTIONS = 10;
    /** 空闲连接最多在连接池保留 5 分钟，超过时间由 OkHttp 回收。 */
    private static final Duration DEFAULT_KEEP_ALIVE = Duration.ofMinutes(5);

    private OkHttpTransportFactory() {
    }

    /**
     * 创建 SDK 持有生命周期的默认 transport。
     *
     * <p>返回的 transport 关闭时会释放本工厂创建的 OkHttpClient 资源，适合最常见的客户接入方式。</p>
     *
     * @return 使用 SDK 默认建连超时和安全策略的 transport
     */
    public static OkHttpTransport create() {
        return create(DEFAULT_CONNECT_TIMEOUT);
    }

    /**
     * 使用指定建连超时创建 SDK 持有生命周期的默认 transport。
     *
     * @param connectTimeout 建连超时，必须为正数且不超过 {@link Integer#MAX_VALUE} 毫秒
     * @return 使用指定建连超时的 transport
     */
    public static OkHttpTransport create(Duration connectTimeout) {
        return new OkHttpTransport(newDefaultClient(connectTimeout), true);
    }

    /**
     * 用客户传入的 OkHttpClient 创建 transport。
     *
     * <p>客户传入 client 时，SDK 默认不接管关闭责任。这样可以支持多个 SDK 或客户业务代码共用同一个
     * OkHttpClient、连接池、代理配置和 TLS 配置。transport 会从该对象派生共享底层资源的安全 client，
     * 强制关闭透明重试和重定向，但不会关闭客户对象。</p>
     *
     * @param client 客户持有生命周期的 OkHttpClient
     * @return 共享客户网络资源、但应用 SDK 安全约束的 transport
     */
    public static OkHttpTransport create(OkHttpClient client) {
        return new OkHttpTransport(hardenCustomerClient(client), false);
    }

    /**
     * 构造 SDK 默认 OkHttpClient。
     *
     * <p>默认配置刻意关闭 OkHttp 自动重试和重定向：重试需要由 core executor 按操作意图判断，
     * 否则设备控制、任务下发等有副作用请求可能被底层透明重发；重定向会让带鉴权信息的 POST
     * 跳到非预期地址，默认也不允许。</p>
     *
     * <p>本方法不配置 trust-all TLS。客户如使用私有 CA，应在传入的 OkHttpClient 中显式配置企业
     * truststore，而不是让 SDK 默认放宽证书校验。</p>
     *
     * @return 由调用方持有生命周期的默认 OkHttpClient
     */
    public static OkHttpClient newDefaultClient() {
        return newDefaultClient(DEFAULT_CONNECT_TIMEOUT);
    }

    /**
     * 使用指定建连超时构造 SDK 默认 OkHttpClient。
     *
     * <p>该重载只改变 connect timeout；透明重试、HTTP/HTTPS 重定向和 TLS 校验策略保持安全默认值。</p>
     *
     * @param connectTimeout 建连超时，必须为正数且不超过 {@link Integer#MAX_VALUE} 毫秒；
     *        小于 1 毫秒的正值按 1 毫秒处理
     * @return 由调用方持有生命周期的默认 OkHttpClient
     */
    public static OkHttpClient newDefaultClient(Duration connectTimeout) {
        long connectTimeoutMillis = OkHttpTimeouts.toMillis(connectTimeout, "connectTimeout");

        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(DEFAULT_MAX_IDLE_CONNECTIONS,
                        DEFAULT_KEEP_ALIVE.toMinutes(), TimeUnit.MINUTES))
                .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .callTimeout(DEFAULT_CALL_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
    }

    /**
     * 从客户 client 派生 SDK 安全配置，同时复用其底层网络资源和观测能力。
     */
    private static OkHttpClient hardenCustomerClient(OkHttpClient client) {
        if (client == null) {
            throw new StationOpenApiClientException("client is required");
        }
        return client.newBuilder()
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
    }
}
