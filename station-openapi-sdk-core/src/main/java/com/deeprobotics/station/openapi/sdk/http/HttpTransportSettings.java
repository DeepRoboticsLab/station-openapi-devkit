package com.deeprobotics.station.openapi.sdk.http;

import java.time.Duration;
import java.util.Objects;

/**
 * 创建默认 transport 所需的非敏感配置。
 *
 * <p>该类型刻意不包含 endpoint、accessToken、appKey 或 secretKey，避免第三方 provider 在创建阶段
 * 获得鉴权材料。</p>
 *
 * @param connectTimeout 建立网络连接的超时时间
 */
public record HttpTransportSettings(Duration connectTimeout) {

    /**
     * 校验默认 transport 的非敏感建连配置。
     *
     * @param connectTimeout 必须为正数的建连超时
     */
    public HttpTransportSettings {
        Objects.requireNonNull(connectTimeout, "connectTimeout is required");
        if (connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
    }
}
