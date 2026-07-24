package com.deeprobotics.station.openapi.sdk.model.request.camera;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 摄像机云台、可见光镜头和红外镜头控制请求。
 *
 * <p>对应 {@code POST /remoteApi/cameraCtrl}。该接口会直接影响现场摄像机状态。
 * 枚举入口覆盖当前已知编码；raw code 入口只排除非正数，以便平台扩展编码时调用方仍可透传。</p>
 */
public final class CameraControlRequest implements OpenApiRequest {

    /**
     * 摄像机编码，通常来自 {@code inventory().pageStreams(...)} 返回的 {@code code}。
     */
    private final String cameraCode;
    /**
     * 控制类型：3=机器人云台，4=可见光镜头，5=红外镜头。
     */
    private final Integer ctrlType;
    /**
     * 控制指令。云台方向当前为 1..8，镜头控制当前主要使用 1..5；SDK 的 raw code 入口允许正整数，
     * 是否受支持由平台校验。
     */
    private final Integer command;
    /**
     * 指令值：0=停止，1=启动。该字段可选；客户传入时必须在 0..1 范围内。
     */
    private final Integer value;
    /**
     * 通道号。可见光或红外镜头控制可能需要；云台控制通常为空。
     */
    private final Integer channelNum;

    private CameraControlRequest(Builder builder) {
        this.cameraCode = builder.cameraCode;
        this.ctrlType = builder.ctrlType;
        this.command = builder.command;
        this.value = builder.value;
        this.channelNum = builder.channelNum;
    }

    /**
     * 创建摄像机控制请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        /*
         * 先做本地校验，再组装 body。这样参数错误不会进入鉴权/签名/HTTP 链路，
         * 调用方拿到的是明确的本地参数异常，而不是平台或设备侧的模糊失败。
         */
        String requiredCameraCode = CameraRequestValidators.requireText("cameraCode", cameraCode);
        Integer requiredCtrlType = CameraRequestValidators.requireInteger("ctrlType", ctrlType);
        Integer requiredCommand = CameraRequestValidators.requireInteger("command", command);
        /*
         * ctrlType/command 的 raw code 是明确的向前兼容逃生口，不能再用当前枚举闭区间把它抵消。
         * 这里只拒绝非正数；平台不支持的编码仍会返回业务错误，由选择 raw 入口的调用方处理。
         */
        CameraRequestValidators.requireRange("ctrlType", requiredCtrlType, 1, Integer.MAX_VALUE);
        CameraRequestValidators.requireRange("command", requiredCommand, 1, Integer.MAX_VALUE);
        CameraRequestValidators.requireRange("value", value, 0, 1);

        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("cameraCode", requiredCameraCode);
        body.put("ctrlType", requiredCtrlType);
        body.put("command", requiredCommand);
        /*
         * value 和 channelNum 是可选字段：value 不传表示不向平台提交启停动作值；
         * channelNum 不传则由平台或设备按当前摄像机上下文处理。null 字段必须省略，避免影响签名源。
         */
        MapUtils.putIfNotNull(body, "value", value);
        MapUtils.putIfNotNull(body, "channelNum", channelNum);
        return body;
    }

    /**
     * 平台当前公开的摄像机控制类型。
     *
     * <p>仍保留 {@link Builder#ctrlType(Integer)} raw code 入口，便于平台扩展时调用方临时透传。</p>
     */
    public enum ControlType {
        /**
         * 机器人云台控制，wire 值为 {@code 3}。
         */
        PTZ(3),
        /**
         * 可见光镜头控制，wire 值为 {@code 4}。
         */
        VISIBLE_LIGHT(4),
        /**
         * 红外镜头控制，wire 值为 {@code 5}。
         */
        INFRARED(5);

        private final int code;

        ControlType(int code) {
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
     * 云台方向控制指令。
     *
     * <p>这些枚举只覆盖平台当前公开的 1..8 方向指令。调用方如果需要临时透传平台新增编码，
     * 可使用 {@link Builder#command(Integer)}。</p>
     */
    public enum PtzCommand {
        /**
         * 云台向上转动，wire 值为 {@code 1}。
         */
        UP(1),
        /**
         * 云台向下转动，wire 值为 {@code 2}。
         */
        DOWN(2),
        /**
         * 云台向左转动，wire 值为 {@code 3}。
         */
        LEFT(3),
        /**
         * 云台向右转动，wire 值为 {@code 4}。
         */
        RIGHT(4),
        /**
         * 云台左上方向转动，wire 值为 {@code 5}。
         */
        LEFT_UP(5),
        /**
         * 云台右上方向转动，wire 值为 {@code 6}。
         */
        RIGHT_UP(6),
        /**
         * 云台左下方向转动，wire 值为 {@code 7}。
         */
        LEFT_DOWN(7),
        /**
         * 云台右下方向转动，wire 值为 {@code 8}。
         */
        RIGHT_DOWN(8);

        private final int code;

        PtzCommand(int code) {
            this.code = code;
        }

        /**
         * 返回发送给平台的云台方向指令码。
         *
         * @return wire 云台指令码
         */
        public int code() {
            return code;
        }
    }

    /**
     * 可见光或红外镜头控制指令。
     */
    public enum LensCommand {
        /**
         * 镜头拉近，wire 值为 {@code 1}。
         */
        ZOOM_IN(1),
        /**
         * 镜头拉远，wire 值为 {@code 2}。
         */
        ZOOM_OUT(2),
        /**
         * 焦距增加，wire 值为 {@code 3}。
         */
        FOCUS_INCREASE(3),
        /**
         * 焦距减少，wire 值为 {@code 4}。
         */
        FOCUS_DECREASE(4),
        /**
         * 自动聚焦，wire 值为 {@code 5}。
         */
        AUTO_FOCUS(5);

        private final int code;

        LensCommand(int code) {
            this.code = code;
        }

        /**
         * 返回发送给平台的镜头指令码。
         *
         * @return wire 镜头指令码
         */
        public int code() {
            return code;
        }
    }

    /**
     * 控制动作值。
     */
    public enum SwitchValue {
        /**
         * 停止当前方向或镜头动作，wire 值为 {@code 0}。
         */
        STOP(0),
        /**
         * 启动当前方向或镜头动作，wire 值为 {@code 1}。
         */
        START(1);

        private final int code;

        SwitchValue(int code) {
            this.code = code;
        }

        /**
         * 返回发送给平台的启停动作值。
         *
         * @return {@code 0} 停止或 {@code 1} 启动
         */
        public int code() {
            return code;
        }
    }

    /**
     * 摄像机控制请求构造器。
     */
    public static final class Builder {
        private String cameraCode;
        private Integer ctrlType;
        private Integer command;
        private Integer value;
        private Integer channelNum;

        private Builder() {
        }

        /**
         * 设置摄像机编码。
         *
         * <p>编码通常来自台账摄像机流分页接口返回的 {@code code}。SDK 会 trim 前后空白，
         * 但不会改变编码大小写或做业务映射。</p>
         *
         * @param cameraCode 摄像机台账编码，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder cameraCode(String cameraCode) {
            this.cameraCode = cameraCode;
            return this;
        }

        /**
         * 使用 SDK 已知控制类型设置 {@code ctrlType}。
         *
         * <p>优先使用枚举入口可以减少客户记忆数字编码的成本，也让调用代码更容易读懂。</p>
         *
         * @param ctrlType SDK 已知控制类型；传 {@code null} 时等待 {@link #build()} 后在发送阶段报必填错误
         * @return 当前构造器
         */
        public Builder ctrlType(ControlType ctrlType) {
            this.ctrlType = ctrlType == null ? null : ctrlType.code();
            return this;
        }

        /**
         * 设置原始控制类型码。
         *
         * <p>保留 raw code 是为了兼容平台扩展；SDK 只校验它为正数，不判断是否属于当前枚举。</p>
         *
         * @param ctrlType 正整数控制类型码
         * @return 当前构造器
         */
        public Builder ctrlType(Integer ctrlType) {
            this.ctrlType = ctrlType;
            return this;
        }

        /**
         * 设置云台方向指令。
         *
         * <p>该入口适用于 {@link ControlType#PTZ}。SDK 不强制校验 ctrlType 与 command 枚举的组合，
         * 是为了保持与平台接口的宽松契约一致。</p>
         *
         * @param command SDK 已知云台方向指令
         * @return 当前构造器
         */
        public Builder command(PtzCommand command) {
            this.command = command == null ? null : command.code();
            return this;
        }

        /**
         * 设置镜头控制指令。
         *
         * <p>该入口适用于可见光或红外镜头。{@code value} 字段本身是可选字段，需要启停语义时再显式设置。</p>
         *
         * @param command SDK 已知镜头控制指令
         * @return 当前构造器
         */
        public Builder command(LensCommand command) {
            this.command = command == null ? null : command.code();
            return this;
        }

        /**
         * 设置原始指令码。SDK 只校验它为正数，是否接受由平台接口决定。
         *
         * @param command 正整数控制指令码
         * @return 当前构造器
         */
        public Builder command(Integer command) {
            this.command = command;
            return this;
        }

        /**
         * 设置控制动作值。
         *
         * <p>方向和镜头连续动作通常需要先发送 START，再在合适时机发送 STOP。SDK 不自动补发停止指令，
         * 由客户业务根据现场交互决定。</p>
         *
         * @param value SDK 已知启停动作；传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder value(SwitchValue value) {
            this.value = value == null ? null : value.code();
            return this;
        }

        /**
         * 设置原始动作值，平台当前校验范围为 0..1。
         *
         * @param value {@code 0} 停止、{@code 1} 启动，或 {@code null} 表示省略字段
         * @return 当前构造器
         */
        public Builder value(Integer value) {
            this.value = value;
            return this;
        }

        /**
         * 设置通道号；不设置时不出现在请求体中。
         *
         * <p>多通道摄像机或红外/可见光切换场景可能需要该字段。若客户不确定通道号，应先以平台台账配置为准。</p>
         *
         * @param channelNum 摄像机通道号，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder channelNum(Integer channelNum) {
            this.channelNum = channelNum;
            return this;
        }

        /**
         * 构建不可变请求对象。
         *
         * <p>字段校验发生在 {@link CameraControlRequest#toMap()}，这样构建对象本身不会触发异常，
         * 便于客户在自己的参数组装流程中先保存 request，再由 SDK 调用时统一校验。</p>
         *
         * @return 不可变的摄像机控制请求
         */
        public CameraControlRequest build() {
            return new CameraControlRequest(this);
        }
    }
}
