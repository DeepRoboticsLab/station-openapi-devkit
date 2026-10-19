package com.deeprobotics.station.openapi.sdk.internal;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.api.DogApi;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
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
import java.util.Objects;
import java.util.Optional;

/**
 * 四足狗 API 默认实现。
 *
 * <p>由 {@link OpenApiExecutor} 执行请求，本类额外将 dogPos 的 JSON 字符串转为坐标对象。</p>
 */
public final class DefaultDogApi implements DogApi {

    private static final EndpointDefinition GET_STATE =
            EndpointDefinition.of("dog.getState", "/remoteApi/getDogStateData", OperationIntent.READ_ONLY);
    private static final EndpointDefinition CHARGE_CONTROL =
            EndpointDefinition.of("dog.chargeControl", "/remoteApi/chargeCtrl", OperationIntent.DEVICE_CONTROL);
    private static final EndpointDefinition CONTROL =
            EndpointDefinition.of("dog.control", "/remoteApi/controlDog", OperationIntent.DEVICE_CONTROL);
    private static final EndpointDefinition SET_POSITION =
            EndpointDefinition.of("dog.setPosition", "/remoteApi/setPosToDog", OperationIntent.DEVICE_CONTROL);
    private static final EndpointDefinition PAGE_ALARMS =
            EndpointDefinition.of("dog.pageAlarms", "/remoteApi/alarmPage", OperationIntent.READ_ONLY);
    private static final EndpointDefinition GET_POSITION =
            EndpointDefinition.of("dog.getPosition", "/remoteApi/dogPos", OperationIntent.READ_ONLY);

    private final OpenApiExecutor executor;
    private final JsonCodec jsonCodec;

    /**
     * 使用 SDK 默认 JSON 编解码器创建四足狗 API 实现。
     *
     * @param executor 已完成客户端级配置的统一执行器
     */
    public DefaultDogApi(OpenApiExecutor executor) {
        this(executor, JsonCodec.defaultCodec());
    }

    DefaultDogApi(OpenApiExecutor executor, JsonCodec jsonCodec) {
        this.executor = Objects.requireNonNull(executor, "executor is required");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "jsonCodec is required");
    }

    @Override
    public DogState getState(DogStateRequest request) {
        return getState(request, null);
    }

    @Override
    public DogState getState(DogStateRequest request, RequestOptions options) {
        DogStateRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(GET_STATE, requiredRequest, new TypeRef<ApiResponse<DogState>>() {
        }, options);
    }

    @Override
    public void chargeControl(DogChargeControlRequest request) {
        chargeControl(request, null);
    }

    @Override
    public void chargeControl(DogChargeControlRequest request, RequestOptions options) {
        DogChargeControlRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        executor.execute(CHARGE_CONTROL, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }

    @Override
    public void control(DogControlRequest request) {
        control(request, null);
    }

    @Override
    public void control(DogControlRequest request, RequestOptions options) {
        DogControlRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        executor.execute(CONTROL, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }

    @Override
    public void setPosition(DogPositionSetRequest request) {
        setPosition(request, null);
    }

    @Override
    public void setPosition(DogPositionSetRequest request, RequestOptions options) {
        DogPositionSetRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        executor.execute(SET_POSITION, requiredRequest, new TypeRef<ApiResponse<Object>>() {
        }, options);
    }

    @Override
    public PageResult<DogAlarmInfo> pageAlarms(DogAlarmPageRequest request) {
        return pageAlarms(request, null);
    }

    @Override
    public PageResult<DogAlarmInfo> pageAlarms(DogAlarmPageRequest request, RequestOptions options) {
        DogAlarmPageRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        return executor.executeRequiredData(PAGE_ALARMS, requiredRequest,
                new TypeRef<ApiResponse<PageResult<DogAlarmInfo>>>() {
        }, options);
    }

    @Override
    public Optional<DogPosition> getPosition(DogPositionRequest request) {
        return getPosition(request, null);
    }

    @Override
    public Optional<DogPosition> getPosition(DogPositionRequest request, RequestOptions options) {
        DogPositionRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        RequestOptions effectiveOptions = executor.prepareOptions(options);
        String rawData = executor.execute(GET_POSITION, requiredRequest, new TypeRef<ApiResponse<String>>() {
        }, effectiveOptions);
        // 平台用空字符串表示暂无坐标。
        if (rawData == null || rawData.isBlank()) {
            return Optional.empty();
        }
        DogPosition position;
        try {
            // 非空 data 必须是 JSON 对象；字面量 null 需另行拒绝，不能视为“暂无坐标”。
            position = jsonCodec.read(rawData, DogPosition.class);
        } catch (StationOpenApiSerializationException ex) {
            throw new StationOpenApiSerializationException("Failed to parse dogPos response",
                    GET_POSITION.path(), effectiveOptions.requestId(), GET_POSITION.operation(), ex);
        }
        if (position == null) {
            throw new StationOpenApiSerializationException("dogPos response JSON root must be an object",
                    GET_POSITION.path(), effectiveOptions.requestId(), GET_POSITION.operation(), null);
        }
        return Optional.of(position);
    }

}
