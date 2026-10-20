package com.deeprobotics.station.openapi.sdk.model.request.task;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.PageRequestSupport;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 任务执行记录分页查询请求。
 *
 * <p>对应 {@code POST /remoteApi/taskPage}。
 * {@code statusArr} 是英文逗号字符串，日期字段按 {@code yyyy-MM-dd} 发送。</p>
 */
public final class TaskPageRequest extends PageRequestSupport {

    /**
     * 四足狗编码过滤条件。
     *
     * <p>为空时不按狗过滤；非空时只查询该狗关联的任务执行记录。发送前会 trim，空白字符串会被省略。</p>
     */
    private final String dogCode;
    /**
     * 任务名称关键字。
     *
     * <p>平台按任务名称做模糊查询。该字段是查询条件，不参与 SDK 侧枚举校验。</p>
     */
    private final String taskName;
    /**
     * 多状态过滤条件。
     *
     * <p>SDK 对调用方暴露集合或逗号字符串两种设置方式，但 wire 字段必须是英文逗号字符串，
     * 因为平台 wire 字段 {@code statusArr} 类型是 {@code String}。平台当前状态码为
     * {@code 1} 已执行、{@code 2} 正在执行、{@code 3} 暂停、{@code 4} 终止、
     * {@code 5} 未执行、{@code 6} 超期。</p>
     */
    private final List<String> statusArr;
    /**
     * 下发状态过滤条件。
     *
     * <p>常见值：0=未下发、1=已下发、2=下发失败。SDK 不在本地限制具体枚举，以兼容平台字典扩展。</p>
     */
    private final Integer sendStatus;
    /**
     * 任务优先级过滤条件。
     *
     * <p>平台当前业务范围为 {@code 1} 最低、{@code 4} 最高，{@code 2/3} 为中间等级。
     * SDK 按 Integer 原值透传。</p>
     */
    private final Integer priority;
    /**
     * 单个任务执行状态过滤条件。
     *
     * <p>码值含义与 {@link #statusArr} 相同。可与 {@code statusArr} 同时使用，平台会把两者合并成
     * 状态集合过滤；SDK 保持字符串原值透传。</p>
     */
    private final String status;
    /**
     * 是否显示未执行任务。
     *
     * <p>调用方不设置时 SDK 会省略该字段，让平台按默认 {@code false} 处理；只有显式设置 true/false 时才发送。</p>
     */
    private final Boolean showUnExecuted;
    /**
     * 计划巡检开始日期过滤。
     *
     * <p>SDK 使用 {@link LocalDate} 表达日期，发送时格式化为平台要求的 {@code yyyy-MM-dd} 字符串，
     * 避免 Java 默认日期格式影响签名和查询结果。</p>
     */
    private final LocalDate patrolStartTime;
    /**
     * 计划巡检结束日期过滤。
     *
     * <p>与 {@link #patrolStartTime} 一样只发送日期，不包含时分秒。</p>
     */
    private final LocalDate patrolEndTime;
    /**
     * SDK 对外页码字段。
     *
     * <p>始终发送 {@code pageNo}，不发送历史 {@code pageNum}。空值会由公共分页基类归一为 1。</p>
     */
    private final Integer pageNo;
    /**
     * SDK 对外分页大小。
     *
     * <p>空值或非正数归一为 10，大于 100 截断为 100，与平台分页规则一致。</p>
     */
    private final Integer pageSize;

    private TaskPageRequest(Builder builder) {
        this.dogCode = builder.dogCode;
        this.taskName = builder.taskName;
        this.statusArr = builder.statusArr;
        this.sendStatus = builder.sendStatus;
        this.priority = builder.priority;
        this.status = builder.status;
        this.showUnExecuted = builder.showUnExecuted;
        this.patrolStartTime = builder.patrolStartTime;
        this.patrolEndTime = builder.patrolEndTime;
        this.pageNo = builder.pageNo;
        this.pageSize = builder.pageSize;
    }

    /**
     * 创建任务执行记录分页请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        /*
         * 任务分页是只读查询，但请求体仍会参与签名。这里按平台 wire 字段顺序构造 LinkedHashMap，
         * 并只写入非空查询条件，保证签名源和最终 JSON body 可预测。
         */
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        MapUtils.putIfNotNull(body, "dogCode", trimToNull(dogCode));
        MapUtils.putIfNotNull(body, "taskName", trimToNull(taskName));
        // statusArr 的 wire 类型是 String，不是数组；过滤空白元素后用英文逗号连接。
        MapUtils.putIfNotNull(body, "statusArr", TaskRequestValidators.joinOptional(statusArr));
        MapUtils.putIfNotNull(body, "sendStatus", sendStatus);
        MapUtils.putIfNotNull(body, "priority", priority);
        MapUtils.putIfNotNull(body, "status", trimToNull(status));
        // 未设置时省略 showUnExecuted，保留平台默认 false 语义。
        MapUtils.putIfNotNull(body, "showUnExecuted", showUnExecuted);
        MapUtils.putIfNotNull(body, "patrolStartTime", TaskRequestValidators.format(patrolStartTime));
        MapUtils.putIfNotNull(body, "patrolEndTime", TaskRequestValidators.format(patrolEndTime));
        // 统一写 pageNo/pageSize；PageRequestSupport 不会写 pageNum。
        putPage(body, pageNo, pageSize);
        return body;
    }

    /**
     * 将可选字符串过滤条件规整为空值或有效文本。
     *
     * <p>查询条件里的空白字符串没有业务意义，省略字段比发送空字符串更接近平台默认查询行为，
     * 也能减少签名排查时的干扰。</p>
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 收集任务执行记录过滤条件和分页参数并创建不可变请求。
     */
    public static final class Builder {
        /** 四足狗编码过滤条件。 */
        private String dogCode;
        /** 任务名称关键字。 */
        private String taskName;
        /** 多状态过滤集合，最终会转换为英文逗号字符串。 */
        private List<String> statusArr;
        /** 下发状态过滤条件。 */
        private Integer sendStatus;
        /** 任务优先级过滤条件。 */
        private Integer priority;
        /** 单个执行状态过滤条件。 */
        private String status;
        /** 是否显示未执行任务；null 表示不发送字段。 */
        private Boolean showUnExecuted;
        /** 计划巡检开始日期。 */
        private LocalDate patrolStartTime;
        /** 计划巡检结束日期。 */
        private LocalDate patrolEndTime;
        /** 页码，从 1 开始；null 使用默认值。 */
        private Integer pageNo;
        /** 每页数量；null/非正数/超过上限会被统一归一。 */
        private Integer pageSize;

        private Builder() {
        }

        /**
         * 设置四足狗编码过滤条件。
         *
         * @param dogCode 四足狗编码；{@code null} 或空白字符串表示不按狗过滤
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 设置任务名称关键字过滤条件。
         *
         * @param taskName 任务名称关键字；{@code null} 或空白字符串表示不按名称过滤
         * @return 当前构造器
         */
        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        /**
         * 使用集合设置多状态过滤条件。
         *
         * <p>集合会被复制，避免请求对象创建后受调用方集合修改影响；最终 wire 仍是逗号字符串。</p>
         *
         * @param statusArr 任务执行状态编码集合；空元素会在发送前过滤
         * @return 当前构造器
         */
        public Builder statusArr(Collection<String> statusArr) {
            this.statusArr = statusArr == null ? null : new ArrayList<>(statusArr);
            return this;
        }

        /**
         * 使用平台兼容的逗号字符串设置多状态过滤条件。
         *
         * <p>该入口便于调用方从历史配置或页面参数中直接传入 {@code "2,3"}；发送前仍会逐段 trim 并过滤空白。</p>
         *
         * @param statusArr 英文逗号分隔的任务执行状态编码
         * @return 当前构造器
         */
        public Builder statusArr(String statusArr) {
            if (statusArr == null) {
                this.statusArr = null;
            } else {
                this.statusArr = List.of(statusArr.split(","));
            }
            return this;
        }

        /**
         * 设置下发状态过滤条件。
         *
         * @param sendStatus 下发状态编码；{@code null} 时不发送该过滤条件
         * @return 当前构造器
         */
        public Builder sendStatus(Integer sendStatus) {
            this.sendStatus = sendStatus;
            return this;
        }

        /**
         * 设置任务优先级过滤条件。
         *
         * @param priority 平台支持的任务优先级；{@code null} 时不发送该过滤条件
         * @return 当前构造器
         */
        public Builder priority(Integer priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 设置单个任务执行状态过滤条件。
         *
         * <p>当前支持 {@code "1"} 已执行、{@code "2"} 正在执行、{@code "3"} 暂停、
         * {@code "4"} 终止、{@code "5"} 未执行、{@code "6"} 超期；SDK 不做枚举限制。</p>
         *
         * @param status 单个任务执行状态编码；{@code null} 或空白字符串表示不按该字段过滤
         * @return 当前构造器
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * 设置是否显示未执行任务。
         *
         * <p>传 {@code null} 或不调用该方法时字段会被省略，平台默认按 {@code false} 处理。</p>
         *
         * @param showUnExecuted 是否显示未执行任务
         * @return 当前构造器
         */
        public Builder showUnExecuted(Boolean showUnExecuted) {
            this.showUnExecuted = showUnExecuted;
            return this;
        }

        /**
         * 设置计划巡检开始日期，发送时格式化为 {@code yyyy-MM-dd}。
         *
         * @param patrolStartTime 计划巡检开始日期；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder patrolStartTime(LocalDate patrolStartTime) {
            this.patrolStartTime = patrolStartTime;
            return this;
        }

        /**
         * 设置计划巡检结束日期，发送时格式化为 {@code yyyy-MM-dd}。
         *
         * @param patrolEndTime 计划巡检结束日期；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder patrolEndTime(LocalDate patrolEndTime) {
            this.patrolEndTime = patrolEndTime;
            return this;
        }

        /**
         * 设置页码；必须大于等于 1，{@code null} 使用默认第 1 页。
         *
         * @param pageNo 页码
         * @return 当前构造器
         */
        public Builder pageNo(Integer pageNo) {
            this.pageNo = pageNo;
            return this;
        }

        /**
         * 设置分页大小；{@code null} 或非正数使用 10，大于 100 截断为 100。
         *
         * @param pageSize 每页数量
         * @return 当前构造器
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * 创建不可变任务分页请求。字段格式化和分页归一会在调用接口时执行。
         *
         * @return 不可变任务执行记录分页请求
         */
        public TaskPageRequest build() {
            return new TaskPageRequest(this);
        }
    }
}
