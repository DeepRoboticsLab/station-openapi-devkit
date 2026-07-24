package com.deeprobotics.station.openapi.sdk.model.request.dog;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 四足狗路网选点下发请求。
 *
 * <p>对应 {@code POST /remoteApi/setPosToDog}。{@code x/y} 的 wire 类型是字符串，
 * SDK 按字符串透传，避免小数精度或签名源发生变化。</p>
 */
public final class DogPositionSetRequest implements OpenApiRequest {

    /**
     * 接收选点下发指令的四足狗编码。
     */
    private final String dogCode;
    /**
     * 路网边编码，通常来自 {@code inventory().listRoadMaps(...)}。
     *
     * <p>平台会按该编码下发狗端可识别的路网信息，因此 SDK 不在本地做路网关系校验。</p>
     */
    private final String edgeCode;
    /**
     * 目标点 X 坐标。wire 类型为字符串，SDK 不转数字。
     */
    private final String x;
    /**
     * 目标点 Y 坐标。wire 类型为字符串，SDK 不转数字。
     */
    private final String y;
    /**
     * 目标点 Z 坐标，是平台接口保留的可选字段；为 {@code null} 时不写入 body。
     */
    private final String z;

    private DogPositionSetRequest(Builder builder) {
        this.dogCode = builder.dogCode;
        this.edgeCode = builder.edgeCode;
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
    }

    /**
     * 创建路网选点下发请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        // x/y 是必填字符串而不是数值类型，避免 BigDecimal/Double 格式化改变签名源或现场坐标精度。
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("dogCode", DogRequestValidators.requireText("dogCode", dogCode));
        body.put("edgeCode", DogRequestValidators.requireText("edgeCode", edgeCode));
        body.put("x", DogRequestValidators.requireText("x", x));
        body.put("y", DogRequestValidators.requireText("y", y));
        MapUtils.putIfNotNull(body, "z", z);
        return body;
    }

    /**
     * 路网选点下发请求构造器。
     */
    public static final class Builder {
        private String dogCode;
        private String edgeCode;
        private String x;
        private String y;
        private String z;

        private Builder() {
        }

        /**
         * 设置四足狗编码。
         *
         * @param dogCode 四足狗编码，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 设置路网边编码。
         *
         * @param edgeCode 路网边编码，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder edgeCode(String edgeCode) {
            this.edgeCode = edgeCode;
            return this;
        }

        /**
         * 设置目标点 X 坐标，按字符串原样透传。
         *
         * @param x 目标点 X 坐标，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder x(String x) {
            this.x = x;
            return this;
        }

        /**
         * 设置目标点 Y 坐标，按字符串原样透传。
         *
         * @param y 目标点 Y 坐标，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder y(String y) {
            this.y = y;
            return this;
        }

        /**
         * 设置目标点 Z 坐标；不设置时不发送该字段。
         *
         * @param z 可选目标点 Z 坐标
         * @return 当前构造器
         */
        public Builder z(String z) {
            this.z = z;
            return this;
        }

        /**
         * 创建不可变的路网选点下发请求。
         *
         * @return 不可变的选点下发请求
         */
        public DogPositionSetRequest build() {
            return new DogPositionSetRequest(this);
        }
    }
}
