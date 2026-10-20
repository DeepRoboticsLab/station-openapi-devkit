package com.deeprobotics.station.openapi.sdk.model.request.inventory;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 机巢停靠点列表查询请求。
 *
 * <p>对应 {@code POST /remoteApi/dockList}。该接口按四足狗编码返回
 * 路网中的导航停靠点，用于巡检点靠近和任务路线规划。响应本身不包含充电能力字段，调用方不能仅凭
 * 停靠点记录判断该位置是否支持充电。</p>
 *
 * <p>{@code dogCode} 是平台必填字段，SDK 在 {@link #toMap()} 中进行本地校验和 trim，
 * 避免空请求体发送到平台并产生难排查的业务错误。</p>
 */
public final class DockListRequest implements OpenApiRequest {

    /**
     * 四足狗编码，对应平台 wire 字段 {@code dogCode}。
     *
     * <p>这是机巢停靠点查询的必填字段。SDK 会在生成请求体时 trim 并校验，
     * 避免空白编码发送到平台并产生难以定位的错误。</p>
     */
    private final String dogCode;

    private DockListRequest(Builder builder) {
        this.dogCode = builder.dogCode;
    }

    /**
     * 创建停靠点列表请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造发送给平台的业务请求体。
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
     * 停靠点列表请求构造器。
     */
    public static final class Builder {
        private String dogCode;

        private Builder() {
        }

        /**
         * 设置四足狗编码。
         *
         * <p>这是机巢停靠点查询的必填过滤条件，构造请求体时会自动去除首尾空格。</p>
         *
         * @param dogCode 四足狗编码，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 创建不可变请求对象。
         *
         * @return 不可变的停靠点列表请求
         */
        public DockListRequest build() {
            return new DockListRequest(this);
        }
    }
}
