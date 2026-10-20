package com.deeprobotics.station.openapi.sdk.model.request.inventory;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 路网节点列表查询请求。
 *
 * <p>对应 {@code POST /remoteApi/nodeList}。该接口按四足狗编码返回
 * 路网节点，调用方可结合 {@code roadMapList} 的边信息绘制完整路线拓扑。</p>
 *
 * <p>{@code dogCode} 是平台必填字段，SDK 在 {@link #toMap()} 中进行本地校验和 trim，
 * 与路网边、机巢停靠点查询保持一致。</p>
 */
public final class NodeListRequest implements OpenApiRequest {

    /**
     * 四足狗编码，对应平台 wire 字段 {@code dogCode}。
     *
     * <p>这是路网节点查询的必填字段。节点列表需要和同一 dogCode 下的路网边一起使用，
     * 因此 SDK 在本地保证该字段非空并输出 trim 后的稳定值。</p>
     */
    private final String dogCode;

    private NodeListRequest(Builder builder) {
        this.dogCode = builder.dogCode;
    }

    /**
     * 创建路网节点列表请求构造器。
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
     * 路网节点列表请求构造器。
     */
    public static final class Builder {
        private String dogCode;

        private Builder() {
        }

        /**
         * 设置四足狗编码。
         *
         * <p>这是路网节点查询的必填过滤条件，构造请求体时会自动去除首尾空格。</p>
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
         * @return 不可变的路网节点列表请求
         */
        public NodeListRequest build() {
            return new NodeListRequest(this);
        }
    }
}
