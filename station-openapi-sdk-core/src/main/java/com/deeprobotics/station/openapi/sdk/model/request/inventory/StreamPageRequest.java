package com.deeprobotics.station.openapi.sdk.model.request.inventory;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.PageRequestSupport;
import java.util.LinkedHashMap;

/**
 * 摄像机流媒体分页查询请求。
 *
 * <p>对应 {@code POST /remoteApi/streamPage}。该接口的精简分页响应只保证
 * {@code records/total} 字段；请求侧仍按标准
 * {@code pageNo/pageSize} 发送，由 SDK 统一处理默认值和上限。</p>
 *
 * <p>{@code name}、{@code bindDeviceCode}、{@code videoMode} 都是可选过滤条件。SDK 不在本地
 * 解释摄像机厂商或码流枚举，以兼容平台增加新值。</p>
 */
public final class StreamPageRequest extends PageRequestSupport {

    /**
     * 摄像机流名称过滤条件，对应平台 wire 字段 {@code name}。
     *
     * <p>字段可选，适合客户在视频列表或设备绑定页面按名称检索。</p>
     */
    private final String name;
    /**
     * 绑定设备编码过滤条件，对应平台 wire 字段 {@code bindDeviceCode}。
     *
     * <p>通常是四足狗编码或摄像机绑定的设备编码，SDK 只负责透传，不推断设备类型。</p>
     */
    private final String bindDeviceCode;
    /**
     * 视频模式过滤条件，对应平台 wire 字段 {@code videoMode}。
     *
     * <p>平台可能扩展编码，SDK 不在本地固定枚举。</p>
     */
    private final String videoMode;
    /**
     * SDK 统一页码字段，对应请求体中的 {@code pageNo}。
     */
    private final Integer pageNo;
    /**
     * SDK 统一每页数量字段，对应请求体中的 {@code pageSize}。
     */
    private final Integer pageSize;

    private StreamPageRequest(Builder builder) {
        this.name = builder.name;
        this.bindDeviceCode = builder.bindDeviceCode;
        this.videoMode = builder.videoMode;
        this.pageNo = builder.pageNo;
        this.pageSize = builder.pageSize;
    }

    /**
     * 创建摄像机流分页请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造发送给平台的业务请求体。
     *
     * <p>字段名保持平台 wire contract：{@code name/bindDeviceCode/videoMode/pageNo/pageSize}。
     * 不发送 {@code null} 字段，减少签名源和 JSON body 的差异点。</p>
     */
    @Override
    public LinkedHashMap<String, Object> toMap() {
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        MapUtils.putIfNotNull(body, "name", name);
        MapUtils.putIfNotNull(body, "bindDeviceCode", bindDeviceCode);
        MapUtils.putIfNotNull(body, "videoMode", videoMode);
        putPage(body, pageNo, pageSize);
        return body;
    }

    /**
     * 收集摄像机流过滤条件和分页参数并创建不可变请求。
     */
    public static final class Builder {
        private String name;
        private String bindDeviceCode;
        private String videoMode;
        private Integer pageNo;
        private Integer pageSize;

        private Builder() {
        }

        /**
         * 按摄像机流名称过滤。
         *
         * @param name 摄像机流名称；{@code null} 时不发送该过滤条件
         * @return 当前构造器
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 按绑定设备编码过滤。
         *
         * <p>通常是狗编码或与摄像机绑定的设备编码，对应平台请求字段 {@code bindDeviceCode}。</p>
         *
         * @param bindDeviceCode 绑定设备编码；{@code null} 时不发送该过滤条件
         * @return 当前构造器
         */
        public Builder bindDeviceCode(String bindDeviceCode) {
            this.bindDeviceCode = bindDeviceCode;
            return this;
        }

        /**
         * 按视频模式过滤。
         *
         * <p>SDK 以原始字符串透传，便于兼容平台扩展的视频模式编码。</p>
         *
         * @param videoMode 平台支持的视频模式原始编码；{@code null} 时不发送该过滤条件
         * @return 当前构造器
         */
        public Builder videoMode(String videoMode) {
            this.videoMode = videoMode;
            return this;
        }

        /**
         * 设置页码，从 1 开始。
         *
         * @param pageNo 页码；{@code null} 时发送默认值 1，小于 1 时在调用接口前失败
         * @return 当前构造器
         */
        public Builder pageNo(Integer pageNo) {
            this.pageNo = pageNo;
            return this;
        }

        /**
         * 设置每页数量，按 SDK 分页规则归一化。
         *
         * @param pageSize 每页数量；{@code null} 或小于等于 0 时归一化为 10，大于 100 时归一化为 100
         * @return 当前构造器
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * 创建不可变请求对象。
         *
         * @return 不可变摄像机流分页请求
         */
        public StreamPageRequest build() {
            return new StreamPageRequest(this);
        }
    }
}
