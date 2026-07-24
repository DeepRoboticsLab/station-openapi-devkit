package com.deeprobotics.station.openapi.sdk.model.request.dog;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 四足狗充电返航控制请求。
 *
 * <p>对应 {@code POST /remoteApi/chargeCtrl}。wire 字段 {@code command} 是字符串，
 * SDK 保持字符串形态发送，不转换成数字。</p>
 */
public final class DogChargeControlRequest implements OpenApiRequest {

    /**
     * 接收充电控制指令的四足狗编码。
     */
    private final String dogCode;
    /**
     * 平台要求的字符串指令码。
     *
     * <p>这里刻意不用 Integer 保存，因为 wire 类型是 {@code String}，签名源和 JSON body
     * 都必须保持 {@code "1"}/{@code "2"} 的字符串形态。</p>
     */
    private final String command;

    private DogChargeControlRequest(Builder builder) {
        this.dogCode = builder.dogCode;
        this.command = builder.command;
    }

    /**
     * 创建充电返航控制请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        // 控制接口在本地完成必填校验，避免无效控制请求进入现场设备链路。
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("dogCode", DogRequestValidators.requireText("dogCode", dogCode));
        body.put("command", DogRequestValidators.requireText("command", command));
        return body;
    }

    /**
     * 平台当前公开的充电控制指令。
     */
    public enum Command {
        /**
         * 充电返航，wire 值为 {@code "1"}。
         */
        RETURN_TO_CHARGE("1"),
        /**
         * 取消返航，wire 值为 {@code "2"}。
         */
        CANCEL_RETURN("2");

        private final String code;

        Command(String code) {
            this.code = code;
        }

        /**
         * 返回发送给平台的字符串指令码。
         *
         * @return wire 指令码
         */
        public String code() {
            return code;
        }
    }

    /**
     * 充电返航控制请求构造器。
     */
    public static final class Builder {
        private String dogCode;
        private String command;

        private Builder() {
        }

        /**
         * 设置四足狗编码，通常来自台账查询结果。
         *
         * @param dogCode 四足狗编码，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 使用 SDK 已知枚举设置充电控制指令，推荐普通客户使用。
         *
         * @param command SDK 已知充电控制指令
         * @return 当前构造器
         */
        public Builder command(Command command) {
            this.command = command == null ? null : command.code();
            return this;
        }

        /**
         * 设置原始指令码，用于平台新增指令时保持 SDK 可用。
         *
         * @param command 非空字符串指令码
         * @return 当前构造器
         */
        public Builder command(String command) {
            this.command = command;
            return this;
        }

        /**
         * 创建不可变的充电返航控制请求。
         *
         * @return 不可变的控制请求
         */
        public DogChargeControlRequest build() {
            return new DogChargeControlRequest(this);
        }
    }
}
