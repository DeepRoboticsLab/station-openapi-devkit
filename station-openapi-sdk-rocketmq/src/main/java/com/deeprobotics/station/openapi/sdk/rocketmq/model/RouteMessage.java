package com.deeprobotics.station.openapi.sdk.rocketmq.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 巡视路线推送消息。
 *
 * <p>对应 {@code uploadHttpRemoteRoute} tag。路线点位 {@code taskPath} 中的每个元素都是
 * {@code x,y,z,a} 四段坐标字符串。默认模式下 {@code x/y/z} 从米换算为厘米，{@code a} 保持原偏航角；
 * 启用像素坐标后 {@code x/y} 为像素值、{@code z} 仍为厘米。SDK 保留字符串，不再拆分。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RouteMessage(
        /** 四足狗编码。 */
        String dogCode,
        /** 四足狗名称。 */
        String dogName,
        /** 任务执行编码，用于关联任务状态和巡检结果。 */
        String taskExecuteCode,
        /** 巡视路线坐标列表。 */
        List<String> taskPath,
        /** 路线关联的巡检点编码列表。 */
        List<String> deviceCodes) {

    /**
     * 把缺失的路线和点位列表归一为空的不可变列表。
     *
     * @param dogCode 四足狗编码
     * @param dogName 四足狗名称
     * @param taskExecuteCode 任务执行编码
     * @param taskPath 路线坐标列表，允许为 {@code null}
     * @param deviceCodes 路线关联点位编码列表，允许为 {@code null}
     */
    public RouteMessage {
        taskPath = taskPath == null ? List.of() : List.copyOf(taskPath);
        deviceCodes = deviceCodes == null ? List.of() : List.copyOf(deviceCodes);
    }
}
