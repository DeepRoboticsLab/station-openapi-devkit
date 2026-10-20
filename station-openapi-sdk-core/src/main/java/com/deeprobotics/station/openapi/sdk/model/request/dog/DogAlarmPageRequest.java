package com.deeprobotics.station.openapi.sdk.model.request.dog;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.PageRequestSupport;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

/**
 * 四足狗本体告警分页查询请求。
 *
 * <p>对应 {@code POST /remoteApi/alarmPage}。SDK 公共分页字段为 {@code pageNo/pageSize}，
 * 时间字段发送前固定为平台要求的字符串格式。</p>
 */
public final class DogAlarmPageRequest extends PageRequestSupport {

    /**
     * 平台告警查询使用的时间格式。
     *
     * <p>不要改成 {@link LocalDateTime#toString()} 的 ISO 格式；平台契约要求
     * {@code yyyy-MM-dd HH:mm:ss}，该字符串同时会进入签名源。</p>
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 可选四足狗编码；不传时由平台查询授权范围内的狗端告警。
     */
    private final String dogCode;
    /**
     * 可选告警开始时间。
     */
    private final LocalDateTime startTime;
    /**
     * 可选告警结束时间。
     */
    private final LocalDateTime endTime;
    /**
     * 可选告警码过滤条件。
     */
    private final String alarmCode;
    /**
     * SDK 统一页码字段，wire body 中不能出现历史字段 {@code pageNum}。
     */
    private final Integer pageNo;
    /**
     * SDK 统一每页数量字段，由 {@link PageRequestSupport} 归一默认值和最大值。
     */
    private final Integer pageSize;

    private DogAlarmPageRequest(Builder builder) {
        this.dogCode = builder.dogCode;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.alarmCode = builder.alarmCode;
        this.pageNo = builder.pageNo;
        this.pageSize = builder.pageSize;
    }

    /**
     * 创建四足狗告警分页请求构造器。
     *
     * @return 新的构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        // alarmPage 的过滤条件均可选，只有分页字段由 SDK 始终写入默认值，确保调用方拿到可预期分页。
        MapUtils.putIfNotNull(body, "dogCode", dogCode);
        if (startTime != null) {
            body.put("startTime", DATE_TIME_FORMATTER.format(startTime));
        }
        if (endTime != null) {
            body.put("endTime", DATE_TIME_FORMATTER.format(endTime));
        }
        MapUtils.putIfNotNull(body, "alarmCode", alarmCode);
        putPage(body, pageNo, pageSize);
        return body;
    }

    /**
     * 四足狗告警分页请求构造器。
     */
    public static final class Builder {
        private String dogCode;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String alarmCode;
        private Integer pageNo;
        private Integer pageSize;

        private Builder() {
        }

        /**
         * 设置四足狗编码过滤条件；不设置则不过滤具体四足狗。
         *
         * @param dogCode 四足狗编码，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 设置告警开始时间，发送时会格式化为 {@code yyyy-MM-dd HH:mm:ss}。
         *
         * @param startTime 告警时间范围起点，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * 设置告警结束时间，发送时会格式化为 {@code yyyy-MM-dd HH:mm:ss}。
         *
         * @param endTime 告警时间范围终点，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        /**
         * 设置告警码过滤条件。
         *
         * @param alarmCode 告警码，传 {@code null} 时省略字段
         * @return 当前构造器
         */
        public Builder alarmCode(String alarmCode) {
            this.alarmCode = alarmCode;
            return this;
        }

        /**
         * 设置页码；不设置时默认第 1 页。
         *
         * @param pageNo 页码
         * @return 当前构造器
         */
        public Builder pageNo(Integer pageNo) {
            this.pageNo = pageNo;
            return this;
        }

        /**
         * 设置每页数量；不设置或传非正数时归一为 10，超过 100 时归一为 100。
         *
         * @param pageSize 每页数量
         * @return 当前构造器
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * 创建不可变的告警分页请求。
         *
         * @return 不可变的告警分页请求
         */
        public DogAlarmPageRequest build() {
            return new DogAlarmPageRequest(this);
        }
    }
}
