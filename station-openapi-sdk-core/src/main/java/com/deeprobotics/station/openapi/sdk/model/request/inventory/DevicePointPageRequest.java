package com.deeprobotics.station.openapi.sdk.model.request.inventory;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.PageRequestSupport;
import java.util.LinkedHashMap;

/**
 * 巡检点分页查询请求。
 *
 * <p>对应 {@code POST /remoteApi/pointPage}。该接口返回巡检业务点位，平台会过滤不属于巡检点的
 * 设备点类型；SDK 请求模型只负责稳定生成 wire 字段。</p>
 *
 * <p>{@code pointCode} 和 {@code dogCode} 都是可选过滤条件。分页字段仍遵守 SDK 统一规则：
 * {@code pageNo} 从 1 开始，{@code pageSize} 默认 10 且最大 100。</p>
 */
public final class DevicePointPageRequest extends PageRequestSupport {

    /**
     * 巡检点编码过滤条件，对应平台 wire 字段 {@code pointCode}。
     *
     * <p>字段可选，常用于调用方从地图点位或结果明细反查指定巡检点的基础资料。</p>
     */
    private final String pointCode;
    /**
     * 四足狗编码过滤条件，对应平台 wire 字段 {@code dogCode}。
     *
     * <p>字段可选，用于限定只查询某台四足狗关联或可见的巡检点。</p>
     */
    private final String dogCode;
    /**
     * SDK 统一页码字段，对应请求体中的 {@code pageNo}。
     *
     * <p>未设置时默认第一页；小于 1 时由公共分页基类抛出本地参数异常。</p>
     */
    private final Integer pageNo;
    /**
     * SDK 统一每页数量字段，对应请求体中的 {@code pageSize}。
     *
     * <p>未设置或非正数按 10 处理，大于 100 会被截断为 100。</p>
     */
    private final Integer pageSize;

    private DevicePointPageRequest(Builder builder) {
        this.pointCode = builder.pointCode;
        this.dogCode = builder.dogCode;
        this.pageNo = builder.pageNo;
        this.pageSize = builder.pageSize;
    }

    /**
     * 创建巡检点分页请求构造器。
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
        MapUtils.putIfNotNull(body, "pointCode", pointCode);
        MapUtils.putIfNotNull(body, "dogCode", dogCode);
        putPage(body, pageNo, pageSize);
        return body;
    }

    /**
     * 巡检点分页请求构造器。
     */
    public static final class Builder {
        private String pointCode;
        private String dogCode;
        private Integer pageNo;
        private Integer pageSize;

        private Builder() {
        }

        /**
         * 按巡检点编码过滤。
         *
         * @param pointCode 巡检点编码，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder pointCode(String pointCode) {
            this.pointCode = pointCode;
            return this;
        }

        /**
         * 按四足狗编码过滤。
         *
         * @param dogCode 四足狗编码，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 设置页码，从 1 开始。
         *
         * @param pageNo 页码，传 {@code null} 时使用第 1 页
         * @return 当前构造器
         */
        public Builder pageNo(Integer pageNo) {
            this.pageNo = pageNo;
            return this;
        }

        /**
         * 设置每页数量，按 SDK 分页规则归一化。
         *
         * @param pageSize 每页数量
         * @return 当前构造器
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * 创建不可变请求对象。
         *
         * @return 不可变的巡检点分页请求
         */
        public DevicePointPageRequest build() {
            return new DevicePointPageRequest(this);
        }
    }
}
