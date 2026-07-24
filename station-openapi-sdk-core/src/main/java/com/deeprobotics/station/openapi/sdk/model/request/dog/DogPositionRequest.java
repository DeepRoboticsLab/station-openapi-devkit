package com.deeprobotics.station.openapi.sdk.model.request.dog;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 四足狗实时坐标查询请求。
 *
 * <p>对应 {@code POST /remoteApi/dogPos}。平台返回 {@code R<String>}，
 * 公共 SDK 方法会继续解析为结构化坐标对象。</p>
 */
public final class DogPositionRequest implements OpenApiRequest {

    /**
     * 需要查询实时坐标的四足狗编码。
     *
     * <p>平台未提供该编码的坐标时返回空串，SDK 公共 API 将其映射为 {@code Optional.empty()}。</p>
     */
    private final String dogCode;

    private DogPositionRequest(Builder builder) {
        this.dogCode = builder.dogCode;
    }

    /**
     * 创建实时坐标查询请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        // dogPos 虽然是只读查询，但平台契约仍要求 POST JSON body，不能改成 GET 或 query 参数。
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("dogCode", DogRequestValidators.requireText("dogCode", dogCode));
        return body;
    }

    /**
     * 实时坐标查询请求构造器。
     */
    public static final class Builder {
        private String dogCode;

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
         * 创建不可变的实时坐标查询请求。
         *
         * @return 不可变的坐标查询请求
         */
        public DogPositionRequest build() {
            return new DogPositionRequest(this);
        }
    }
}
