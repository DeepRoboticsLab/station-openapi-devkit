package com.deeprobotics.station.openapi.sdk.model.request.inventory;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 地图列表查询请求。
 *
 * <p>对应 {@code POST /remoteApi/mapList}。该接口不分页，常用于
 * 客户在页面初始化或设备切换时加载指定楼层、指定狗可用的地图文件信息。</p>
 *
 * <p>{@code floor} 和 {@code dogCode} 都是可选过滤条件。未传字段不会进入 JSON body，也不会参与签名。</p>
 */
public final class MapListRequest implements OpenApiRequest {

    /**
     * 楼层过滤条件，对应平台 wire 字段 {@code floor}。
     *
     * <p>字段可选；未传时由平台按授权范围返回地图列表。</p>
     */
    private final String floor;
    /**
     * 四足狗编码过滤条件，对应平台 wire 字段 {@code dogCode}。
     *
     * <p>字段可选，常用于只加载某台四足狗当前可用或绑定的地图。</p>
     */
    private final String dogCode;

    private MapListRequest(Builder builder) {
        this.floor = builder.floor;
        this.dogCode = builder.dogCode;
    }

    /**
     * 创建地图列表请求构造器。
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
        MapUtils.putIfNotNull(body, "floor", floor);
        MapUtils.putIfNotNull(body, "dogCode", dogCode);
        return body;
    }

    /**
     * 地图列表请求构造器。
     */
    public static final class Builder {
        private String floor;
        private String dogCode;

        private Builder() {
        }

        /**
         * 按楼层过滤地图。
         *
         * @param floor 楼层或地图编码，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder floor(String floor) {
            this.floor = floor;
            return this;
        }

        /**
         * 按四足狗编码过滤地图。
         *
         * @param dogCode 四足狗编码，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 创建不可变请求对象。
         *
         * @return 不可变的地图列表请求
         */
        public MapListRequest build() {
            return new MapListRequest(this);
        }
    }
}
