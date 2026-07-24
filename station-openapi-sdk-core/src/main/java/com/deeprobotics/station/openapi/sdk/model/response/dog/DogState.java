package com.deeprobotics.station.openapi.sdk.model.response.dog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 四足狗状态汇总。
 *
 * <p>平台返回电池、定位、基础状态等对外分组，
 * SDK 保留原始分组名和状态项，不在本地翻译或重命名 wire 值。</p>
 *
 * <p>{@code props} 在构造时会被复制成不可变列表。这样客户拿到响应后不能意外修改 SDK 返回对象，
 * 也能避免调试时同一个响应对象在不同代码路径看到不同状态。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DogState(
        /**
         * 四足狗编码。
         *
         * <p>该值用于和台账接口 {@code DogInfo.dogCode}、
         * 坐标接口 {@link DogPosition#dogCode()} 等结果互相关联。SDK 不对编码做本地格式假设，
         * 客户应按平台返回值原样保存或展示。</p>
         */
        String dogCode,
        /**
         * 四足狗型号。
         *
         * <p>该值可用于展示或按机型分支处理状态项，但 SDK 的公共逻辑不依赖具体型号，
         * 以兼容平台新增型号。</p>
         */
        String dogModel,
        /**
         * 状态分组列表。
         *
         * <p>平台当前会输出电池、定位、基础状态等对外分组，但调用方不应假设分组数量固定；
         * 平台新增只读状态分组时，SDK 会通过
         * {@link JsonIgnoreProperties} 和该列表自然承载。</p>
         */
        List<DogStateGroup> props) {

    /**
     * 平台归一后的充电状态项 key。
     *
     * <p>不要用分组名或中文文案判断充电状态；分组和展示文案可能随语言包变化，但该 status key
     * 是当前平台 API 对外契约的一部分。</p>
     */
    private static final String CHARGE_STATE_STATUS = "batteryData.chargeState.title";
    /**
     * 平台把不同机型的“正在充电”统一输出为字符串 {@code "1"}。
     */
    private static final String CHARGING_VALUE = "1";

    /**
     * 创建状态汇总，并把缺失的状态分组归一为空的不可变列表。
     *
     * @param dogCode 四足狗编码
     * @param dogModel 四足狗型号
     * @param props 状态分组列表，允许为 {@code null}
     */
    public DogState {
        // 对外统一为空列表而不是 null，便于客户直接 stream/for-each，同时保持响应对象不可变。
        props = props == null ? List.of() : List.copyOf(props);
    }

    /**
     * 判断平台归一后的充电状态是否为充电中。
     *
     * <p>平台会把不同机型的充电状态统一成字符串 {@code "1"} 或 {@code "0"}。
     * SDK 只基于这个稳定对外值判断，不读取内部机型枚举。</p>
     *
     * @return 任一状态项表示正在充电时返回 {@code true}
     */
    public boolean isCharging() {
        return props.stream()
                .flatMap(group -> group.list().stream())
                .anyMatch(item -> CHARGE_STATE_STATUS.equals(item.status())
                        && CHARGING_VALUE.equals(item.value()));
    }
}
