package com.deeprobotics.station.openapi.sample.service;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogAlarmPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogChargeControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogPositionRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogPositionSetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogStateRequest;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogAlarmInfo;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogPosition;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogState;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 直接展示 {@code client.dog()} 分组的查询与控制调用。
 *
 * <p>这里不为控制操作增加自动重试，调用方应结合查询结果或 MQ 状态判断现场执行情况。</p>
 */
@Service
public class DogSampleService {

    private final StationOpenApiClient client;

    public DogSampleService(StationOpenApiClient client) {
        this.client = client;
    }

    public DogState getState(DogStateRequest request, RequestOptions options) {
        return client.dog().getState(request, options);
    }

    public void chargeControl(DogChargeControlRequest request, RequestOptions options) {
        client.dog().chargeControl(request, options);
    }

    public void control(DogControlRequest request, RequestOptions options) {
        client.dog().control(request, options);
    }

    public void setPosition(DogPositionSetRequest request, RequestOptions options) {
        client.dog().setPosition(request, options);
    }

    public PageResult<DogAlarmInfo> pageAlarms(DogAlarmPageRequest request, RequestOptions options) {
        return client.dog().pageAlarms(request, options);
    }

    public Optional<DogPosition> getPosition(DogPositionRequest request, RequestOptions options) {
        return client.dog().getPosition(request, options);
    }
}
