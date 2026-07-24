package com.deeprobotics.station.openapi.sdk.model.request.camera;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 摄像机云台复位请求。
 *
 * <p>对应 {@code POST /remoteApi/ptzReset}。请求体只有
 * {@code cameraCode} 一个业务字段；鉴权字段由 SDK 执行器统一追加。</p>
 */
public final class CameraResetRequest implements OpenApiRequest {

    /**
     * 需要复位云台的摄像机编码。
     */
    private final String cameraCode;

    private CameraResetRequest(Builder builder) {
        this.cameraCode = builder.cameraCode;
    }

    /**
     * 创建云台复位请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        // 云台复位只有 cameraCode 一个业务字段，签名字段或 Token header 由执行器统一处理。
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("cameraCode", CameraRequestValidators.requireText("cameraCode", cameraCode));
        return body;
    }

    /**
     * 云台复位请求构造器。
     */
    public static final class Builder {
        private String cameraCode;

        private Builder() {
        }

        /**
         * 设置摄像机编码。
         *
         * <p>SDK 会在发送前 trim 前后空白并校验非空；编码值本身按平台台账原样透传。</p>
         *
         * @param cameraCode 摄像机台账编码，不能为 null 或空白
         * @return 当前构造器
         */
        public Builder cameraCode(String cameraCode) {
            this.cameraCode = cameraCode;
            return this;
        }

        /**
         * 构建云台复位请求。
         *
         * <p>请求对象不可变；实际必填校验延迟到 {@link CameraResetRequest#toMap()} 执行。</p>
         *
         * @return 不可变的云台复位请求
         */
        public CameraResetRequest build() {
            return new CameraResetRequest(this);
        }
    }
}
