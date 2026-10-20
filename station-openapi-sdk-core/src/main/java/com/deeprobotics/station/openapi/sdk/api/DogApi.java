package com.deeprobotics.station.openapi.sdk.api;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
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

/**
 * 四足狗状态、告警、坐标和控制 API 分组。
 *
 * <p>状态、告警和坐标查询按 READ_ONLY 操作处理；充电、运动控制和选点下发按 DEVICE_CONTROL
 * 操作处理且不自动重试。</p>
 *
 * <p>{@code dogPos} 的平台响应将坐标放在字符串形式的 {@code data} 中，SDK 将其转换为
 * 结构化坐标模型或空结果，不向调用方暴露原始 JSON 字符串。</p>
 */
public interface DogApi {

    /**
     * 查询四足狗状态分组。
     *
     * <p>对应 {@code POST /remoteApi/getDogStateData}。平台返回电池、定位、
     * 基础状态等稳定对外分组，SDK 不翻译 {@code groupName/status/value} 等字段，便于调用方按现场语言包
     * 或自己的展示逻辑处理。</p>
     *
     * @param request 状态查询条件，必须包含非空 {@code dogCode}
     * @return 非 null 的四足狗状态汇总
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    DogState getState(DogStateRequest request);

    /**
     * 查询四足狗状态分组，并为本次调用指定请求级选项。
     *
     * @param request 状态查询条件，必须包含非空 {@code dogCode}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的四足狗状态汇总
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    DogState getState(DogStateRequest request, RequestOptions options);

    /**
     * 下发充电返航或取消返航指令。
     *
     * <p>对应 {@code POST /remoteApi/chargeCtrl}。这是设备控制接口，SDK 不自动重试；
     * 如果调用超时或连接状态不确定，异常会标记结果未知，调用方应查询狗状态或任务状态确认现场结果。</p>
     *
     * @param request 充电控制请求，必须包含非空 {@code dogCode} 和 {@code command}
     */
    void chargeControl(DogChargeControlRequest request);

    /**
     * 下发充电返航或取消返航指令，并为本次调用指定请求级选项。
     *
     * @param request 充电控制请求，必须包含非空 {@code dogCode} 和 {@code command}
     * @param options 请求 ID 和超时覆盖配置；{@code null} 使用默认配置，不能开启自动重试
     */
    void chargeControl(DogChargeControlRequest request, RequestOptions options);

    /**
     * 下发四足狗模式切换或运动控制指令。
     *
     * <p>对应 {@code POST /remoteApi/controlDog}。SDK 提供枚举入口避免调用方记忆数字编码，
     * 同时保留 raw code setter 以兼容平台新增指令。{@code type=2} 的运动控制会在本地校验
     * {@code value} 是数值且不大于 {@code 0.6}。</p>
     *
     * @param request 控制请求，必须包含非空 {@code dogCode/type/command/value}
     */
    void control(DogControlRequest request);

    /**
     * 下发四足狗模式切换或运动控制指令，并为本次调用指定请求级选项。
     *
     * @param request 控制请求，必须包含非空 {@code dogCode/type/command/value}
     * @param options 请求 ID 和超时覆盖配置；{@code null} 使用默认配置，不能开启自动重试
     */
    void control(DogControlRequest request, RequestOptions options);

    /**
     * 向四足狗下发路网选点坐标。
     *
     * <p>对应 {@code POST /remoteApi/setPosToDog}。{@code x/y/z} 均按字符串处理，
     * 避免浮点格式化改变签名源或现场坐标精度；其中 {@code z} 是可选字段。</p>
     *
     * @param request 选点下发请求，必须包含非空 {@code dogCode/edgeCode/x/y}
     */
    void setPosition(DogPositionSetRequest request);

    /**
     * 向四足狗下发路网选点坐标，并为本次调用指定请求级选项。
     *
     * @param request 选点下发请求，必须包含非空 {@code dogCode/edgeCode/x/y}
     * @param options 请求 ID 和超时覆盖配置；{@code null} 使用默认配置，不能开启自动重试
     */
    void setPosition(DogPositionSetRequest request, RequestOptions options);

    /**
     * 分页查询四足狗本体告警。
     *
     * <p>对应 {@code POST /remoteApi/alarmPage}。分页字段固定为 {@code pageNo/pageSize}，
     * 时间字段由请求模型格式化为 {@code yyyy-MM-dd HH:mm:ss}。</p>
     *
     * @param request 告警分页查询条件，不能为 {@code null}
     * @return 非 null 的四足狗本体告警分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<DogAlarmInfo> pageAlarms(DogAlarmPageRequest request);

    /**
     * 分页查询四足狗本体告警，并为本次调用指定请求级选项。
     *
     * @param request 告警分页查询条件，不能为 {@code null}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 非 null 的四足狗本体告警分页结果
     * @throws StationOpenApiSerializationException 平台成功响应未包含 {@code data}
     */
    PageResult<DogAlarmInfo> pageAlarms(DogAlarmPageRequest request, RequestOptions options);

    /**
     * 查询四足狗实时坐标。
     *
     * <p>对应 {@code POST /remoteApi/dogPos}。平台成功响应中的 {@code data} 是 JSON 字符串、
     * {@code null} 或空白字符串；SDK 将有效 JSON 解析为 {@link DogPosition}，无坐标数据时返回
     * {@link Optional#empty()}。</p>
     *
     * @param request 坐标查询条件，必须包含非空 {@code dogCode}
     * @return 实时坐标；平台未提供坐标数据时为空
     * @throws StationOpenApiSerializationException 非空 {@code data} 不是有效的坐标 JSON 对象
     */
    Optional<DogPosition> getPosition(DogPositionRequest request);

    /**
     * 查询四足狗实时坐标，并为本次调用指定请求级选项。
     *
     * @param request 坐标查询条件，必须包含非空 {@code dogCode}
     * @param options 本次调用的 requestId、超时或重试覆盖配置；传 {@code null} 时使用客户端默认配置
     * @return 实时坐标；平台未提供坐标数据时为空
     * @throws StationOpenApiSerializationException 非空 {@code data} 不是有效的坐标 JSON 对象
     */
    Optional<DogPosition> getPosition(DogPositionRequest request, RequestOptions options);
}
