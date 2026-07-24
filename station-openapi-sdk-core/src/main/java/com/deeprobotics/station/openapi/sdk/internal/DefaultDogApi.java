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
 * <p>本类只做业务方法到平台 API path 的映射和少量 SDK 侧结果转换。鉴权、JSON 请求发送、
 * {@code R<T>} 解包、重试和异常上下文都由 {@link OpenApiExecutor} 统一完成。</p>
 */
public final class DefaultDogApi implements DogApi {

    /**
     * 四足狗接口的路径和操作意图定义。
     *
     * <p>执行器根据 {@link OperationIntent} 决定默认重试和结果未知语义：
     * 状态、告警、坐标查询是只读；充电、运动控制和选点下发会影响现场设备，默认不能自动重试。</p>
     */
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
    /**
     * 专用于 {@code dogPos} 的二次 JSON 解析。
     *
     * <p>绝大多数平台接口由 {@link OpenApiExecutor} 直接把响应 {@code data} 解析成目标类型；
     * {@code /remoteApi/dogPos} 是例外：平台响应的 {@code data} 本身是一个 JSON 字符串，因此需要在
     * 执行器解包出 {@link String} 之后，再由本类把字符串解析成 {@link DogPosition}。</p>
     */
    private final JsonCodec jsonCodec;

    /**
     * 使用 SDK 默认 JSON 编解码器创建四足狗 API 实现。
     *
     * <p>生产代码走这个构造器；包内构造器只给测试传入定制 codec 使用，避免公开额外扩展点。</p>
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
        // null 请求对象没有稳定 wire body，也会绕过请求模型中的 dogCode 校验，因此在进入执行器前拦截。
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
        // 充电控制是现场设备操作，EndpointDefinition 已标记 DEVICE_CONTROL，执行器不会默认重试。
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
        // 运动控制的必填字段、speed 范围和 type=2 的 value 上限都在请求模型 toMap() 中统一校验。
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
        // 选点下发会改变狗端运动目标，和直接运动控制一样按 DEVICE_CONTROL 处理。
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
        // 告警分页是只读查询，分页默认值和时间格式化由 DogAlarmPageRequest 负责。
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
        // 第一步按字符串 data 调用，保留平台现有 wire contract，不要求平台改成对象返回。
        DogPositionRequest requiredRequest = ClientInputs.requireNonNull(request, "request");
        RequestOptions effectiveOptions = executor.prepareOptions(options);
        String rawData = executor.execute(GET_POSITION, requiredRequest, new TypeRef<ApiResponse<String>>() {
        }, effectiveOptions);
        // 平台没有可用坐标时返回空串；调用方更需要“无坐标”语义，而不是空 JSON 字符串。
        if (rawData == null || rawData.isBlank()) {
            return Optional.empty();
        }
        DogPosition position;
        try {
            /*
             * 外层 R.data 是字符串，内层必须是 JSON object。Jackson 会把 JSON 字面量 null 解析成
             * Java null；它既不是平台约定的“无坐标”空串，也不能交给 Optional.of，否则会泄漏裸 NPE。
             * 其他 scalar/array 根节点由 Jackson 拒绝，最终都统一成带 dogPos 上下文的 SDK 序列化异常。
             */
            position = jsonCodec.read(rawData, DogPosition.class);
        } catch (StationOpenApiSerializationException ex) {
            // 补齐 path/operation；客户显式传入 requestId 时也一并保留，便于定位 dogPos 响应格式异常。
            throw new StationOpenApiSerializationException("Failed to parse dogPos response",
                    GET_POSITION.path(), effectiveOptions.requestId(), GET_POSITION.operation(), ex);
        }
        if (position == null) {
            throw new StationOpenApiSerializationException("dogPos response JSON root must be an object",
                    GET_POSITION.path(), effectiveOptions.requestId(), GET_POSITION.operation(), null);
        }
        // 第二步只暴露 DogPosition 中的稳定字段，未知字段由模型注解忽略。
        return Optional.of(position);
    }

}
