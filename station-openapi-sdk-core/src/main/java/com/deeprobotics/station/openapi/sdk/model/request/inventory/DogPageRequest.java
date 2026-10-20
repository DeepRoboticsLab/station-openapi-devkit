package com.deeprobotics.station.openapi.sdk.model.request.inventory;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.PageRequestSupport;
import java.util.LinkedHashMap;

/**
 * 四足狗台账分页查询请求。
 *
 * <p>对应 {@code POST /remoteApi/dogPage}。SDK 对外只使用 {@code pageNo/pageSize}，
 * 不会发送平台兼容字段 {@code pageNum}。分页默认值、
 * 非法页码和页大小上限由 {@link PageRequestSupport#putPage(LinkedHashMap, Integer, Integer)}
 * 统一处理，确保请求体、签名源和文档示例保持一致。</p>
 *
 * <p>{@code dogName} 和 {@code deviceMode} 都是可选过滤条件。字段为 {@code null} 时不进入
 * 请求体；空字符串如果由调用方显式传入会保留在 body 中，但签名算法会按规则跳过空白字符串。</p>
 */
public final class DogPageRequest extends PageRequestSupport {

    /**
     * SDK 统一页码字段，对应请求体中的 {@code pageNo}。
     *
     * <p>调用方不传时由 {@link PageRequestSupport} 归一为第一页；如果传入小于 1 的值，
     * SDK 会在本地抛出参数异常，避免无效分页请求发送到平台。</p>
     */
    private final Integer pageNo;
    /**
     * SDK 统一每页数量字段，对应请求体中的 {@code pageSize}。
     *
     * <p>调用方不传、传 0 或负数时归一为 10；超过 100 时归一为 100，防止单次查询过大影响现场系统。</p>
     */
    private final Integer pageSize;
    /**
     * 四足狗名称过滤条件，对应平台 wire 字段 {@code dogName}。
     *
     * <p>该字段可选，{@code null} 不进入请求体；空字符串会按调用方显式输入保留在 body 中。</p>
     */
    private final String dogName;
    /**
     * 设备型号过滤条件，对应平台 wire 字段 {@code deviceMode}。
     *
     * <p>SDK 不限制型号枚举，按字符串透传，便于兼容现场新增机型。</p>
     */
    private final String deviceMode;

    private DogPageRequest(Builder builder) {
        this.pageNo = builder.pageNo;
        this.pageSize = builder.pageSize;
        this.dogName = builder.dogName;
        this.deviceMode = builder.deviceMode;
    }

    /**
     * 创建四足狗台账分页请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造发送给平台的业务请求体。
     *
     * <p>返回 {@link LinkedHashMap} 是为了保证字段插入顺序稳定，便于调试时对比请求体；
     * 实际签名排序仍由鉴权模块按 Java 字符串字典序完成。</p>
     */
    @Override
    public LinkedHashMap<String, Object> toMap() {
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        putPage(body, pageNo, pageSize);
        MapUtils.putIfNotNull(body, "dogName", dogName);
        MapUtils.putIfNotNull(body, "deviceMode", deviceMode);
        return body;
    }

    /**
     * 四足狗台账分页请求构造器。
     */
    public static final class Builder {
        private Integer pageNo;
        private Integer pageSize;
        private String dogName;
        private String deviceMode;

        private Builder() {
        }

        /**
         * 设置页码，从 1 开始。
         *
         * <p>传 {@code null} 时归一为 1；小于 1 时在本地抛出参数异常。</p>
         *
         * @param pageNo 页码
         * @return 当前构造器
         */
        public Builder pageNo(Integer pageNo) {
            this.pageNo = pageNo;
            return this;
        }

        /**
         * 设置每页数量。
         *
         * <p>传 {@code null}、0 或负数时归一为 10；大于 100 时归一为 100。</p>
         *
         * @param pageSize 每页数量
         * @return 当前构造器
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * 按四足狗名称过滤。
         *
         * <p>该字段对应平台请求字段 {@code dogName}，通常用于按设备名称模糊检索。</p>
         *
         * @param dogName 四足狗名称关键字，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder dogName(String dogName) {
            this.dogName = dogName;
            return this;
        }

        /**
         * 按设备型号过滤。
         *
         * <p>该字段对应平台请求字段 {@code deviceMode}，例如按 X30、M20 等型号筛选狗台账。</p>
         *
         * @param deviceMode 设备型号，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder deviceMode(String deviceMode) {
            this.deviceMode = deviceMode;
            return this;
        }

        /**
         * 创建不可变请求对象。
         *
         * @return 不可变的四足狗台账分页请求
         */
        public DogPageRequest build() {
            return new DogPageRequest(this);
        }
    }
}
