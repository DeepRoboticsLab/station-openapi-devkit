package com.deeprobotics.station.openapi.sdk.model.request.inventory;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 路网列表查询请求。
 *
 * <p>对应 {@code POST /remoteApi/roadMapList}。该接口按四足狗编码返回
 * 该狗所在地图的路网边信息，供调用方绘制可通行路线或进行选点前校验。</p>
 *
 * <p>{@code dogCode} 是平台必填字段，SDK 在 {@link #toMap()} 中先做非空和去首尾空格处理，
 * 使调用方可在本地立即得到参数异常，无需等待平台返回业务错误。</p>
 */
public final class RoadMapListRequest implements OpenApiRequest {

    /**
     * 四足狗编码，对应平台 wire 字段 {@code dogCode}。
     *
     * <p>这是路网边查询的必填字段。{@link #toMap()} 会做非空、非空白校验并去除首尾空格，
     * 保证最终请求体中的编码可直接参与平台查询和签名。</p>
     */
    private final String dogCode;

    private RoadMapListRequest(Builder builder) {
        this.dogCode = builder.dogCode;
    }

    /**
     * 创建路网列表请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造发送给平台的业务请求体。
     *
     * <p>返回 body 一定包含规范化后的 {@code dogCode}；如果调用方未传或只传空白字符，
     * 会抛出 {@code StationOpenApiClientException}。</p>
     */
    @Override
    public LinkedHashMap<String, Object> toMap() {
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("dogCode", requireDogCode(dogCode));
        return body;
    }

    private static String requireDogCode(String dogCode) {
        if (dogCode == null || dogCode.trim().isEmpty()) {
            throw new StationOpenApiClientException("dogCode is required");
        }
        return dogCode.trim();
    }

    /**
     * 收集路网查询条件并创建不可变请求。
     */
    public static final class Builder {
        private String dogCode;

        private Builder() {
        }

        /**
         * 设置四足狗编码。
         *
         * <p>这是路网查询的必填过滤条件，构造请求体时会自动去除首尾空格。</p>
         *
         * @param dogCode 四足狗编码
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 创建不可变请求对象。
         *
         * @return 不可变路网列表请求
         */
        public RoadMapListRequest build() {
            return new RoadMapListRequest(this);
        }
    }
}
