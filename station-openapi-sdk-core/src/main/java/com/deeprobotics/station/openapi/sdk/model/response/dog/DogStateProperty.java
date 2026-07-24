package com.deeprobotics.station.openapi.sdk.model.response.dog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 四足狗单个状态项。
 *
 * <p>字段按平台原值透传，便于调用方按自己的展示和告警逻辑解释。例如 {@code status} 是状态项 key，
 * {@code value} 是平台归一后的字符串值，{@code remark} 和 {@code alarmCondition}
 * 可用于展示取值范围或告警阈值说明。</p>
 *
 * <p>这里没有把 {@code alarm} 改成 primitive boolean，是为了准确表达平台未返回字段或返回 null 的情况；
 * 调用方如果需要默认值，应在自己的展示层决定。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DogStateProperty(
        /**
         * 状态项 key。
         *
         * <p>常见值包括 {@code batteryData.chargeState.title}、
         * {@code posX}、{@code yaw} 等。它用于稳定识别某个状态项，不是最终展示文案；
         * 调用方可以基于该 key 做本地化展示。</p>
         */
        String status,
        /**
         * 状态值。
         *
         * <p>该字段始终按字符串透传。不同状态项的业务含义不同，可能是数值、布尔编码、
         * 枚举编码或坐标文本。SDK 不在这里统一转换类型，避免把平台状态配置
         * 固定在 SDK 枚举里。</p>
         */
        String value,
        /**
         * 状态备注或取值说明。
         *
         * <p>常用于说明枚举值含义、有效范围或现场展示提示；可能为空，调用方展示层应自行决定是否显示。</p>
         */
        String remark,
        /**
         * 告警触发条件说明。
         *
         * <p>通常是平台配置生成的阈值表达式或判断说明。SDK 只透传文本，不在客户端解析表达式，
         * 避免与平台告警规则产生不一致。</p>
         */
        String alarmCondition,
        /**
         * 当前状态项是否触发告警或异常。
         *
         * <p>使用 {@link Boolean} 是为了保留平台未返回该字段时的 {@code null} 语义；
         * 调用方若需要默认 false，应在自己的业务层显式处理。</p>
         */
        Boolean alarm,
        /**
         * 状态值单位。
         *
         * <p>例如百分比、电压、角度或距离单位。并非所有状态项都有单位，
         * 因此该字段可能为 {@code null} 或空字符串。</p>
         */
        String unit) {
}
