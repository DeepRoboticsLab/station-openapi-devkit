package com.deeprobotics.station.openapi.sdk.model.request.result;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 地图或巡检结果文件下载请求。
 *
 * <p>对应 {@code POST /remoteApi/result/resource}。
 * {@code type=1} 表示地图文件，{@code type=2} 表示巡检结果图片或其他结果资源。</p>
 */
public final class ResourceDownloadRequest implements OpenApiRequest {

    /**
     * 要下载的文件路径或文件名。
     *
     * <p>地图文件通常来自地图列表中的地图文件名；巡检结果资源通常来自
     * {@code PatrolResultInfo.fileList().get(i).filePath()}。这里传的是平台返回的资源路径，
     * 不是调用方本机的绝对路径；调用方通过输出流决定数据去向。</p>
     */
    private final String filePath;
    /**
     * 平台资源类型编码。
     *
     * <p>wire 值只能是 1 或 2：1 表示地图文件，2 表示巡检结果图片或其他结果资源。
     * SDK 在 {@link #toMap()} 中本地校验，避免把明显非法类型发到平台。</p>
     */
    private final Integer type;

    private ResourceDownloadRequest(Builder builder) {
        this.filePath = builder.filePath;
        this.type = builder.type;
    }

    /**
     * 创建资源下载请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        /*
         * 下载接口虽然返回二进制流，但请求体仍是 JSON，并且同样参与签名。
         * 这里不能写入本地 target 路径，只能写平台接口需要的 filePath/type。
         */
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("filePath", ResultRequestValidators.requireText("filePath", filePath));
        body.put("type", ResultRequestValidators.requireResourceType(type));
        return body;
    }

    /**
     * 构造资源下载请求。
     */
    public static final class Builder {
        /**
         * 平台侧资源路径或地图文件名。
         */
        private String filePath;
        /**
         * 平台资源类型编码，最终只能为 1 或 2。
         */
        private Integer type;

        private Builder() {
        }

        /**
         * 设置平台侧资源路径。
         *
         * @param filePath 平台可识别的文件路径或文件名，最终发送前会 trim，空白值会失败
         * @return 当前 builder
         */
        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        /**
         * 直接设置平台资源类型编码。
         *
         * <p>保留 Integer 入口便于调用方从配置或平台数据中透传类型值；发送前仍会限制为 1 或 2。</p>
         *
         * @param type 平台资源类型编码：1 表示地图文件，2 表示巡检结果资源
         * @return 当前构造器
         */
        public Builder type(Integer type) {
            this.type = type;
            return this;
        }

        /**
         * 使用 SDK 枚举设置资源类型。
         *
         * <p>这是推荐入口，能让调用代码直接表达“地图文件”或“巡检结果资源”，减少记忆数字编码的成本。</p>
         *
         * @param type SDK 资源类型；传入 {@code null} 会在接口调用时按缺少必填参数处理
         * @return 当前构造器
         */
        public Builder type(Type type) {
            this.type = type == null ? null : type.wireValue();
            return this;
        }

        /**
         * 创建不可变下载请求。filePath/type 的必填和范围校验会在接口调用时执行。
         *
         * @return 不可变下载请求
         */
        public ResourceDownloadRequest build() {
            return new ResourceDownloadRequest(this);
        }
    }

    /**
     * 资源下载类型。wire 仍发送平台要求的整数 1 或 2。
     */
    public enum Type {
        /**
         * 地图文件，wire 值为 1。
         */
        MAP_FILE(1),
        /**
         * 巡检结果图片或其他结果资源，wire 值为 2。
         */
        RESULT_RESOURCE(2);

        /**
         * 平台接口 {@code type} 字段接收的整数编码。
         */
        private final int wireValue;

        Type(int wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * 返回平台 wire 编码。
         *
         * @return 平台接口 {@code type} 字段接收的整数编码
         */
        public int wireValue() {
            return wireValue;
        }
    }
}
