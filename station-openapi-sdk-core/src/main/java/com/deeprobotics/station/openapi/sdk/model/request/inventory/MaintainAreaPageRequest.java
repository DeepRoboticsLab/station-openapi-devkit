package com.deeprobotics.station.openapi.sdk.model.request.inventory;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.PageRequestSupport;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/**
 * 检修区域分页查询请求。
 *
 * <p>对应 {@code POST /remoteApi/maintainAreaPage}。该接口
 * 用于查询地图上的维保/检修区域配置，常用于客户在地图或工单系统中定位需要人工处理的区域。</p>
 *
 * <p>{@code startTime/endTime} 在 SDK 公共 API 中使用 {@link LocalDateTime}，发送前固定格式化为
 * {@code yyyy-MM-dd HH:mm:ss}。不要改成 {@code LocalDateTime.toString()}，否则 JSON body 和签名源
 * 会与平台 wire 契约不一致。</p>
 */
public final class MaintainAreaPageRequest extends PageRequestSupport {

    /**
     * 平台维保区域查询要求的日期时间格式。
     *
     * <p>该格式会同时影响请求 JSON 和签名源，不能替换成 Java 默认 ISO 字符串。</p>
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 四足狗编码过滤条件，对应平台 wire 字段 {@code dogCode}。
     *
     * <p>字段可选，用于查询指定四足狗所在地图或授权范围内的维保区域。</p>
     */
    private final String dogCode;
    /**
     * 维保区域编码过滤条件，对应平台 wire 字段 {@code maintainAreaCode}。
     *
     * <p>字段可选，适合客户从工单、地图区域点击后精确查询某个区域。</p>
     */
    private final String maintainAreaCode;
    /**
     * 维修或维保描述过滤条件，对应平台 wire 字段 {@code repairDesc}。
     *
     * <p>SDK 不做语义拆分或模糊规则处理，只把调用方输入透传给平台。</p>
     */
    private final String repairDesc;
    /**
     * 查询开始时间，对应平台 wire 字段 {@code startTime}。
     *
     * <p>公共 API 使用 {@link LocalDateTime}，发送前固定格式化为 {@code yyyy-MM-dd HH:mm:ss}。</p>
     */
    private final LocalDateTime startTime;
    /**
     * 查询结束时间，对应平台 wire 字段 {@code endTime}。
     *
     * <p>字段可选；SDK 不在本地校验起止时间先后关系，保持与平台查询语义一致。</p>
     */
    private final LocalDateTime endTime;
    /**
     * 下发状态过滤条件，对应平台 wire 字段 {@code issued}。
     *
     * <p>平台当前定义 {@code 0} 为未下发、{@code 1} 为已下发；传 {@code null} 时不按下发状态过滤。
     * SDK 保持 Integer 原值透传，不在客户端改写过滤条件。</p>
     */
    private final Integer issued;
    /**
     * 楼层过滤条件，对应平台 wire 字段 {@code floor}。
     *
     * <p>字段可选，通常用于多楼层地图或楼宇场景下缩小查询范围。</p>
     */
    private final String floor;
    /**
     * SDK 统一页码字段，对应请求体中的 {@code pageNo}。
     */
    private final Integer pageNo;
    /**
     * SDK 统一每页数量字段，对应请求体中的 {@code pageSize}。
     */
    private final Integer pageSize;

    private MaintainAreaPageRequest(Builder builder) {
        this.dogCode = builder.dogCode;
        this.maintainAreaCode = builder.maintainAreaCode;
        this.repairDesc = builder.repairDesc;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.issued = builder.issued;
        this.floor = builder.floor;
        this.pageNo = builder.pageNo;
        this.pageSize = builder.pageSize;
    }

    /**
     * 创建检修区域分页请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造发送给平台的业务请求体。
     *
     * <p>时间字段在这里完成 wire 格式转换，分页字段在最后统一写入，保证调试时请求体字段顺序稳定。</p>
     */
    @Override
    public LinkedHashMap<String, Object> toMap() {
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        MapUtils.putIfNotNull(body, "dogCode", dogCode);
        MapUtils.putIfNotNull(body, "maintainAreaCode", maintainAreaCode);
        MapUtils.putIfNotNull(body, "repairDesc", repairDesc);
        if (startTime != null) {
            body.put("startTime", DATE_TIME_FORMATTER.format(startTime));
        }
        if (endTime != null) {
            body.put("endTime", DATE_TIME_FORMATTER.format(endTime));
        }
        MapUtils.putIfNotNull(body, "issued", issued);
        MapUtils.putIfNotNull(body, "floor", floor);
        putPage(body, pageNo, pageSize);
        return body;
    }

    /**
     * 检修区域分页请求构造器。
     */
    public static final class Builder {
        private String dogCode;
        private String maintainAreaCode;
        private String repairDesc;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer issued;
        private String floor;
        private Integer pageNo;
        private Integer pageSize;

        private Builder() {
        }

        /**
         * 按四足狗编码过滤维保区域。
         *
         * @param dogCode 四足狗编码，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 按维保区域编码过滤。
         *
         * @param maintainAreaCode 维保区域编码，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder maintainAreaCode(String maintainAreaCode) {
            this.maintainAreaCode = maintainAreaCode;
            return this;
        }

        /**
         * 按维修或维保描述过滤。
         *
         * <p>对应平台请求字段 {@code repairDesc}，SDK 只做字符串透传。</p>
         *
         * @param repairDesc 维修或维保描述，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder repairDesc(String repairDesc) {
            this.repairDesc = repairDesc;
            return this;
        }

        /**
         * 设置查询开始时间。
         *
         * <p>发送时格式化为 {@code yyyy-MM-dd HH:mm:ss}，不携带时区信息。</p>
         *
         * @param startTime 查询时间范围起点，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * 设置查询结束时间。
         *
         * <p>发送时格式化为 {@code yyyy-MM-dd HH:mm:ss}，不携带时区信息。</p>
         *
         * @param endTime 查询时间范围终点，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        /**
         * 按下发状态过滤。
         *
         * <p>该字段对应平台请求字段 {@code issued}：{@code 0} 未下发、{@code 1} 已下发；传 {@code null}
         * 时省略字段。SDK 不在本地限制整数范围，平台新增码值时仍可由调用方显式透传。</p>
         *
         * @param issued 下发状态过滤码
         * @return 当前构造器
         */
        public Builder issued(Integer issued) {
            this.issued = issued;
            return this;
        }

        /**
         * 按楼层过滤。
         *
         * @param floor 楼层或地图编码，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder floor(String floor) {
            this.floor = floor;
            return this;
        }

        /**
         * 设置页码，从 1 开始。
         *
         * @param pageNo 页码
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
         * @return 不可变的检修区域分页请求
         */
        public MaintainAreaPageRequest build() {
            return new MaintainAreaPageRequest(this);
        }
    }
}
