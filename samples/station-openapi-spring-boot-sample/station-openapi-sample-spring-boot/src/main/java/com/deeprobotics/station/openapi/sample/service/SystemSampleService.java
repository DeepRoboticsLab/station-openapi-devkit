package com.deeprobotics.station.openapi.sample.service;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import org.springframework.stereotype.Service;

/**
 * 直接展示 {@code client.system()} 分组的无业务参数调用。
 *
 * <p>该 Service 展示单例 Client 与 {@link RequestOptions} 的用法。</p>
 */
@Service
public class SystemSampleService {

    private final StationOpenApiClient client;

    public SystemSampleService(StationOpenApiClient client) {
        this.client = client;
    }

    public String getTimeText(RequestOptions options) {
        return client.system().getTimeText(options);
    }
}
