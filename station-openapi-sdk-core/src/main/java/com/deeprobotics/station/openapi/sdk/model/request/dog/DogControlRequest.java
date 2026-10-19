package com.deeprobotics.station.openapi.sdk.model.request.dog;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;

/**
 * 四足狗运动控制请求。
 *
 * <p>对应 {@code POST /remoteApi/controlDog}。这是设备控制接口，SDK 在发出请求前完成参数校验，
 * 避免明显危险或无效的控制命令进入现场系统。</p>
 */
public final class DogControlRequest implements OpenApiRequest {

    /**
     * 平台对 {@code type=2} 运动控制开放的最大控制值。
     *
     * <p>使用 {@link BigDecimal} 而不是 double，避免 {@code 0.6} 这类十进制边界在比较时出现浮点误差。</p>
     */
    private static final BigDecimal MAX_MOTION_VALUE = new BigDecimal("0.6");

    /**
     * 接收控制指令的四足狗编码。
     */
    private final String dogCode;
    /**
     * 控制类型：1=模式切换，2=车体运动控制。
     */
    private final Integer type;
    /**
     * 控制指令码。具体含义取决于 {@link #type}。
     */
    private final Integer command;
    /**
     * 指令值，wire 类型为字符串，SDK 默认发送 {@code "0"}。
     *
     * <p>即使调用方不显式设置，也必须最终发送该平台必填字段。</p>
     */
    private final String value;
    /**
     * 指令重复下发次数。
     *
     * <p>字段名虽为 {@code speed}，其实际含义是重复下发次数；不传时平台按 1 次处理。</p>
     */
    private final Integer speed;

    private DogControlRequest(Builder builder) {
        this.dogCode = builder.dogCode;
        this.type = builder.type;
        this.command = builder.command;
        this.value = builder.value;
        this.speed = builder.speed;
    }

    /**
     * 创建四足狗运动控制请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        // 先做所有本地校验，再组装 body，确保失败时不会产生半成品请求体或进入 HTTP 层。
        String requiredDogCode = DogRequestValidators.requireText("dogCode", dogCode);
        Integer requiredType = DogRequestValidators.requireInteger("type", type);
        Integer requiredCommand = DogRequestValidators.requireInteger("command", command);
        String requiredValue = DogRequestValidators.requireText("value", value);
        validateSpeed(speed);
        validateMotionValue(requiredType, requiredValue);

        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("dogCode", requiredDogCode);
        body.put("type", requiredType);
        body.put("command", requiredCommand);
        body.put("value", requiredValue);
        // speed 可选：不发送时使用平台默认值 1；发送时必须已经通过 1..5 校验。
        MapUtils.putIfNotNull(body, "speed", speed);
        return body;
    }

    private static void validateSpeed(Integer speed) {
        if (speed != null && (speed < 1 || speed > 5)) {
            throw new StationOpenApiClientException("speed must be between 1 and 5");
        }
    }

    private static void validateMotionValue(Integer type, String value) {
        if (!Integer.valueOf(Type.MOTION_CONTROL.code()).equals(type)) {
            return;
        }
        // type=2 直接进入狗端运动控制，SDK 在本地执行平台约定的数值校验，尽早返回明确的参数错误。
        BigDecimal numericValue;
        try {
            numericValue = new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new StationOpenApiClientException("value must be numeric when type is 2", ex);
        }
        if (numericValue.compareTo(MAX_MOTION_VALUE) > 0) {
            throw new StationOpenApiClientException("value must be less than or equal to 0.6 when type is 2");
        }
    }

    /**
     * 平台当前公开的控制类型。
     *
     * <p>保留 {@link Builder#type(Integer)} raw code 入口，以兼容平台新增类型。</p>
     */
    public enum Type {
        /**
         * 控制模式切换，wire 值为 {@code 1}。
         */
        MODE_SWITCH(1),
        /**
         * 车体运动控制，wire 值为 {@code 2}。
         */
        MOTION_CONTROL(2);

        private final int code;

        Type(int code) {
            this.code = code;
        }

        /**
         * 返回发送给平台的控制类型码。
         *
         * @return wire 控制类型码
         */
        public int code() {
            return code;
        }
    }

    /**
     * 模式切换常用指令。
     *
     * <p>平台当前只公开 {@code command=6}，具体切到任务模式还是遥控模式由 {@code value} 表示。</p>
     */
    public enum ModeCommand {
        /**
         * 切换控制模式，wire 值为 {@code 6}；具体目标模式由 {@code value} 决定。
         */
        SWITCH_MODE(6);

        private final int code;

        ModeCommand(int code) {
            this.code = code;
        }

        /**
         * 返回发送给平台的模式切换指令码。
         *
         * @return wire 模式指令码
         */
        public int code() {
            return code;
        }
    }

    /**
     * 运动控制常用指令。
     *
     * <p>这些编码来自平台公开接口约定。对于尚未进入 SDK 枚举的新编码，
     * 仍可使用 {@link Builder#command(Integer)} 透传。</p>
     */
    public enum MotionCommand {
        /**
         * 取消当前选点目标或运动目标，wire 值为 {@code 0}。
         */
        CANCEL_TARGET(0),
        /**
         * 前进，wire 值为 {@code 1}。
         */
        FORWARD(1),
        /**
         * 后退，wire 值为 {@code 2}。
         */
        BACKWARD(2),
        /**
         * 左转，wire 值为 {@code 3}。
         */
        TURN_LEFT(3),
        /**
         * 右转，wire 值为 {@code 4}。
         */
        TURN_RIGHT(4),
        /**
         * 左移，wire 值为 {@code 11}。
         */
        LEFT(11),
        /**
         * 右移，wire 值为 {@code 12}。
         */
        RIGHT(12),
        /**
         * 趴下，wire 值为 {@code 15}；可沿用请求默认 {@code value="0"}。
         */
        LIE_DOWN(15),
        /**
         * 起立，wire 值为 {@code 16}；可沿用请求默认 {@code value="0"}。
         */
        STAND_UP(16);

        private final int code;

        MotionCommand(int code) {
            this.code = code;
        }

        /**
         * 返回发送给平台的运动控制指令码。
         *
         * @return wire 运动指令码
         */
        public int code() {
            return code;
        }
    }

    /**
     * 四足狗运动控制请求构造器。
     */
    public static final class Builder {
        private String dogCode;
        private Integer type;
        private Integer command;
        private String value = "0";
        private Integer speed;

        private Builder() {
        }

        /**
         * 设置接收控制指令的四足狗编码。
         *
         * @param dogCode 四足狗编码，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 使用 SDK 已知控制类型设置 {@code type}，推荐普通客户使用。
         *
         * @param type SDK 已知控制类型
         * @return 当前构造器
         */
        public Builder type(Type type) {
            this.type = type == null ? null : type.code();
            return this;
        }

        /**
         * 设置原始控制类型码，用于平台新增类型时保持 SDK 可用。
         *
         * @param type 原始控制类型码
         * @return 当前构造器
         */
        public Builder type(Integer type) {
            this.type = type;
            return this;
        }

        /**
         * 设置模式切换指令。
         *
         * @param command SDK 已知模式切换指令
         * @return 当前构造器
         */
        public Builder command(ModeCommand command) {
            this.command = command == null ? null : command.code();
            return this;
        }

        /**
         * 设置运动控制指令。
         *
         * @param command SDK 已知运动控制指令
         * @return 当前构造器
         */
        public Builder command(MotionCommand command) {
            this.command = command == null ? null : command.code();
            return this;
        }

        /**
         * 设置原始控制指令码，用于平台新增指令时保持 SDK 可用。
         *
         * @param command 原始控制指令码
         * @return 当前构造器
         */
        public Builder command(Integer command) {
            this.command = command;
            return this;
        }

        /**
         * 设置指令值；不设置时发送 SDK 默认值 {@code "0"}。
         *
         * @param value 指令值；{@code type=2} 时必须是小于等于 0.6 的数值字符串
         * @return 当前构造器
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * 设置重复下发次数，平台允许范围为 1..5；不设置则不发送，由平台按 1 次处理。
         *
         * @param speed 重复下发次数，允许为 {@code null}
         * @return 当前构造器
         */
        public Builder speed(Integer speed) {
            this.speed = speed;
            return this;
        }

        /**
         * 创建不可变的四足狗运动控制请求。
         *
         * @return 不可变的运动控制请求
         */
        public DogControlRequest build() {
            return new DogControlRequest(this);
        }
    }
}
