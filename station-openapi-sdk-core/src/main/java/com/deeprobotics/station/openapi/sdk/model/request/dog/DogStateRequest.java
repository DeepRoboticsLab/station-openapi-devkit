package com.deeprobotics.station.openapi.sdk.model.request.dog;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 四足狗状态查询请求。
 *
 * <p>对应 {@code POST /remoteApi/getDogStateData}。{@code dogCode} 是平台必填字段，
 * SDK 在构造 wire body 时提前做本地校验。</p>
 */
public final class DogStateRequest implements OpenApiRequest {

    /**
     * 四足狗编码，对应平台请求中的同名字段 {@code dogCode}。
     *
     * <p>该字段参与签名和 JSON body 构造。客户传入首尾空格时会在 {@link #toMap()} 中被 trim，
     * 既减少现场排查成本，也避免“看起来相同但签名源不同”的问题。</p>
     */
    private final String dogCode;

    private DogStateRequest(Builder builder) {
        this.dogCode = builder.dogCode;
    }

    /**
     * 创建四足狗状态查询请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        // 请求模型只负责产生平台 wire body；鉴权字段 appKey/timestamp/nonce/sign 由 AuthProvider 后置追加。
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("dogCode", DogRequestValidators.requireText("dogCode", dogCode));
        return body;
    }

    /**
     * 四足狗状态查询请求构造器。
     */
    public static final class Builder {
        private String dogCode;

        private Builder() {
        }

        /**
         * 设置四足狗编码。
         *
         * <p>编码通常来自 {@code inventory().pageDogs(...)} 返回的 {@code dogCode}。</p>
         *
         * @param dogCode 四足狗编码，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 创建不可变的四足狗状态查询请求。
         *
         * @return 不可变的状态查询请求
         */
        public DogStateRequest build() {
            return new DogStateRequest(this);
        }
    }
}
