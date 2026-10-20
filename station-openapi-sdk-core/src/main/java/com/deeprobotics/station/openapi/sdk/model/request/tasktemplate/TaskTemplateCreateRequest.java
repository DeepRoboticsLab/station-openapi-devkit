package com.deeprobotics.station.openapi.sdk.model.request.tasktemplate;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * 新增任务模板请求。
 *
 * <p>对应 {@code POST /remoteApi/onlySaveTaskModel}。该接口只保存模板，
 * 不立即下发执行；成功后返回平台生成的 {@code taskCode}。</p>
 *
 * <p>注意多值字段的 wire 形态：{@code pointCodes/cycleMonth/cycleWeek} 都是字符串，
 * SDK 对外提供集合 builder 只是为了方便调用方组装，最终请求体必须输出英文逗号分隔字符串。</p>
 */
public final class TaskTemplateCreateRequest implements OpenApiRequest {

    /**
     * SDK 默认发送的结束动作：自动充电。
     *
     * <p>SDK 显式发送 {@code "1"}，使创建模板时的结束动作不依赖平台隐式默认值。</p>
     */
    private static final String DEFAULT_END_ACTION = "1";

    /**
     * 任务模板名称，必填。
     *
     * <p>用于平台列表、详情展示和调用方排查。SDK 发送前会 trim 前后空白。</p>
     */
    private final String taskName;
    /**
     * 执行类型，按平台编码透传：1=立即，2=定时，3=周期，4=间隔。
     */
    private final Integer executeType;
    /**
     * 巡检类型，按平台字符串编码透传。
     *
     * <p>不同部署环境可能存在扩展编码，SDK 不在这里枚举约束。</p>
     */
    private final String patrolType;
    /**
     * 巡检点编码集合的 wire 字段，必填，最终发送为英文逗号字符串。
     *
     * <p>虽然 builder 支持 {@code Collection<String>}，这里保存的仍是 wire contract 要求的字符串形态。</p>
     */
    private final String pointCodes;
    /**
     * 任务优先级，按平台数值透传。
     */
    private final Integer priority;
    /**
     * 任务结束动作，默认 {@code "1"} 自动充电。
     *
     * <p>调用方如果要保持在原地或使用其他平台支持动作，可通过 builder 传入原始字符串编码。</p>
     */
    private final String endAction;
    /**
     * 设备等级，按平台数值透传。
     */
    private final Integer deviceLevel;
    /**
     * 是否启用，按平台字符串编码透传：{@code "1"} 启用、{@code "0"} 停用。
     */
    private final String isEnable;
    /**
     * 定时任务开始时间。
     *
     * <p>发送前格式化为 {@code yyyy-MM-dd HH:mm:ss}，避免 Java 默认 ISO 格式与 wire contract 不一致。</p>
     */
    private final LocalDateTime fixedStartTime;
    /**
     * 周期月份，最终发送为英文逗号字符串。
     */
    private final String cycleMonth;
    /**
     * 周期星期，最终发送为英文逗号字符串。
     */
    private final String cycleWeek;
    /**
     * 周期任务每日执行时间，发送格式为 {@code HH:mm:ss}。
     */
    private final LocalTime cycleExecuteTime;
    /**
     * 周期任务生效开始时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
     */
    private final LocalDateTime cycleStartTime;
    /**
     * 周期任务生效结束时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
     */
    private final LocalDateTime cycleEndTime;
    /**
     * 间隔任务的间隔数量。
     */
    private final Integer intervalNumber;
    /**
     * 间隔任务的间隔单位类型，按平台编码透传，例如 1=小时、2=天、3=分钟。
     */
    private final Integer intervalType;
    /**
     * 间隔任务参考执行时间，发送格式为 {@code HH:mm:ss}。
     */
    private final LocalTime intervalExecuteTime;
    /**
     * 间隔任务生效开始时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
     */
    private final LocalDateTime intervalStartTime;
    /**
     * 间隔任务生效结束时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
     */
    private final LocalDateTime intervalEndTime;
    /**
     * 模板整体不可用开始时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
     */
    private final LocalDateTime invalidStartTime;
    /**
     * 模板整体不可用结束时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
     */
    private final LocalDateTime invalidEndTime;
    /**
     * 每天不可用开始时间，发送格式为 {@code HH:mm:ss}。
     */
    private final LocalTime invalidDailyStartTime;
    /**
     * 每天不可用结束时间，发送格式为 {@code HH:mm:ss}。
     */
    private final LocalTime invalidDailyEndTime;

    private TaskTemplateCreateRequest(Builder builder) {
        this.taskName = builder.taskName;
        this.executeType = builder.executeType;
        this.patrolType = builder.patrolType;
        this.pointCodes = builder.pointCodes;
        this.priority = builder.priority;
        this.endAction = builder.endAction;
        this.deviceLevel = builder.deviceLevel;
        this.isEnable = builder.isEnable;
        this.fixedStartTime = builder.fixedStartTime;
        this.cycleMonth = builder.cycleMonth;
        this.cycleWeek = builder.cycleWeek;
        this.cycleExecuteTime = builder.cycleExecuteTime;
        this.cycleStartTime = builder.cycleStartTime;
        this.cycleEndTime = builder.cycleEndTime;
        this.intervalNumber = builder.intervalNumber;
        this.intervalType = builder.intervalType;
        this.intervalExecuteTime = builder.intervalExecuteTime;
        this.intervalStartTime = builder.intervalStartTime;
        this.intervalEndTime = builder.intervalEndTime;
        this.invalidStartTime = builder.invalidStartTime;
        this.invalidEndTime = builder.invalidEndTime;
        this.invalidDailyStartTime = builder.invalidDailyStartTime;
        this.invalidDailyEndTime = builder.invalidDailyEndTime;
    }

    /**
     * 创建任务模板新增请求构造器。
     *
     * @return 带有默认结束动作的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        /*
         * 按 wire 字段顺序组装 LinkedHashMap，便于测试和排查请求体。
         * 签名阶段会按 key 排序，这里的插入顺序不影响签名结果，但稳定输出能降低调试成本。
         */
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("taskName", TaskTemplateRequestValidators.requireText("taskName", taskName));
        MapUtils.putIfNotNull(body, "executeType", executeType);
        MapUtils.putIfNotNull(body, "patrolType", patrolType);
        // pointCodes 是必填字段，且 wire 类型是逗号字符串，不是 JSON 数组。
        body.put("pointCodes", TaskTemplateRequestValidators.requireCommaText("pointCodes", pointCodes));
        MapUtils.putIfNotNull(body, "priority", priority);
        // endAction 始终发送；未设置时使用 SDK 默认值。
        body.put("endAction", TaskTemplateRequestValidators.requireText("endAction", endAction));
        MapUtils.putIfNotNull(body, "deviceLevel", deviceLevel);
        MapUtils.putIfNotNull(body, "isEnable", isEnable);
        // 所有 Java time 字段在进入 body 前就格式化为平台期望字符串，签名源也使用同一份字符串值。
        MapUtils.putIfNotNull(body, "fixedStartTime", TaskTemplateRequestValidators.format(fixedStartTime));
        MapUtils.putIfNotNull(body, "cycleMonth", cycleMonth);
        MapUtils.putIfNotNull(body, "cycleWeek", cycleWeek);
        MapUtils.putIfNotNull(body, "cycleExecuteTime", TaskTemplateRequestValidators.format(cycleExecuteTime));
        MapUtils.putIfNotNull(body, "cycleStartTime", TaskTemplateRequestValidators.format(cycleStartTime));
        MapUtils.putIfNotNull(body, "cycleEndTime", TaskTemplateRequestValidators.format(cycleEndTime));
        MapUtils.putIfNotNull(body, "intervalNumber", intervalNumber);
        MapUtils.putIfNotNull(body, "intervalType", intervalType);
        MapUtils.putIfNotNull(body, "intervalExecuteTime", TaskTemplateRequestValidators.format(intervalExecuteTime));
        MapUtils.putIfNotNull(body, "intervalStartTime", TaskTemplateRequestValidators.format(intervalStartTime));
        MapUtils.putIfNotNull(body, "intervalEndTime", TaskTemplateRequestValidators.format(intervalEndTime));
        MapUtils.putIfNotNull(body, "invalidStartTime", TaskTemplateRequestValidators.format(invalidStartTime));
        MapUtils.putIfNotNull(body, "invalidEndTime", TaskTemplateRequestValidators.format(invalidEndTime));
        MapUtils.putIfNotNull(body, "invalidDailyStartTime",
                TaskTemplateRequestValidators.format(invalidDailyStartTime));
        MapUtils.putIfNotNull(body, "invalidDailyEndTime", TaskTemplateRequestValidators.format(invalidDailyEndTime));
        return body;
    }

    /**
     * 收集任务模板字段并创建不可变请求。
     *
     * <p>构造器保留平台可选字段的 {@code null} 状态；只有必填字段和格式规则会在接口调用时校验。</p>
     */
    public static final class Builder {
        private String taskName;
        private Integer executeType;
        private String patrolType;
        private String pointCodes;
        private Integer priority;
        private String endAction = DEFAULT_END_ACTION;
        private Integer deviceLevel;
        private String isEnable;
        private LocalDateTime fixedStartTime;
        private String cycleMonth;
        private String cycleWeek;
        private LocalTime cycleExecuteTime;
        private LocalDateTime cycleStartTime;
        private LocalDateTime cycleEndTime;
        private Integer intervalNumber;
        private Integer intervalType;
        private LocalTime intervalExecuteTime;
        private LocalDateTime intervalStartTime;
        private LocalDateTime intervalEndTime;
        private LocalDateTime invalidStartTime;
        private LocalDateTime invalidEndTime;
        private LocalTime invalidDailyStartTime;
        private LocalTime invalidDailyEndTime;

        private Builder() {
        }

        /**
         * 设置任务模板名称，必填。
         *
         * <p>SDK 会在发送前 trim 前后空白；中间空格按调用方输入保留。</p>
         *
         * @param taskName 任务模板名称
         * @return 当前构造器
         */
        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        /**
         * 设置执行类型：1=立即，2=定时，3=周期，4=间隔。
         *
         * <p>该字段不做枚举硬编码，避免平台新增执行类型时 SDK 阻断调用方临时透传。</p>
         *
         * @param executeType 平台支持的执行类型编码；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder executeType(Integer executeType) {
            this.executeType = executeType;
            return this;
        }

        /**
         * 设置巡检类型，SDK 按平台原始字符串透传。
         *
         * @param patrolType 平台支持的巡检类型原始编码；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder patrolType(String patrolType) {
            this.patrolType = patrolType;
            return this;
        }

        /**
         * 设置巡检点编码集合，发送时会拼成英文逗号字符串。
         *
         * <p>该字段是新增模板必填项。集合中的 null、空字符串和全空白字符串会被过滤；
         * 如果最终没有有效编码，发送时抛本地参数异常。</p>
         *
         * @param pointCodes 巡检点编码集合
         * @return 当前构造器
         */
        public Builder pointCodes(Collection<String> pointCodes) {
            this.pointCodes = TaskTemplateRequestValidators.joinOptional(pointCodes);
            return this;
        }

        /**
         * 设置巡检点编码逗号字符串。
         *
         * <p>该字段是新增模板必填项。这个入口用于调用方已经保存了平台 wire 形态的场景；
         * 发送前仍会拆分、trim 并重新用英文逗号拼接。</p>
         *
         * @param pointCodes 英文逗号分隔的巡检点编码
         * @return 当前构造器
         */
        public Builder pointCodes(String pointCodes) {
            this.pointCodes = pointCodes;
            return this;
        }

        /**
         * 设置任务优先级。
         *
         * <p>平台当前业务范围为 {@code 1} 最低、{@code 4} 最高，{@code 2/3} 为中间等级；
         * SDK 按 Integer 原值透传。</p>
         *
         * @param priority 平台支持的任务优先级；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder priority(Integer priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 设置任务结束动作；不设置时默认 {@code "1"}，即自动充电。
         *
         * <p>传入空字符串会在发送时被判定为非法，避免模板结束动作不可预期。</p>
         *
         * @param endAction 平台支持的结束动作编码
         * @return 当前构造器
         */
        public Builder endAction(String endAction) {
            this.endAction = endAction;
            return this;
        }

        /**
         * 设置设备等级。
         *
         * <p>设备等级用于平台调度或过滤，SDK 不解释业务含义，按数值原样传输。</p>
         *
         * @param deviceLevel 平台支持的设备等级；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder deviceLevel(Integer deviceLevel) {
            this.deviceLevel = deviceLevel;
            return this;
        }

        /**
         * 设置是否启用，按平台字符串编码透传。
         *
         * <p>{@code "1"} 表示启用，{@code "0"} 表示停用；传 {@code null} 时省略该字段。</p>
         *
         * @param isEnable 启用状态原始编码
         * @return 当前构造器
         */
        public Builder isEnable(String isEnable) {
            this.isEnable = isEnable;
            return this;
        }

        /**
         * 设置定时任务开始时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
         *
         * <p>只对定时类模板有业务意义；其他执行类型传入后也会按平台字段透传。</p>
         *
         * @param fixedStartTime 定时任务开始时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder fixedStartTime(LocalDateTime fixedStartTime) {
            this.fixedStartTime = fixedStartTime;
            return this;
        }

        /**
         * 设置周期月份集合，发送时会拼成英文逗号字符串。
         *
         * <p>月份编码按平台规则透传，例如 {@code 1,2,12}。SDK 只负责过滤空值和拼接。</p>
         *
         * @param cycleMonth 周期月份编码集合；{@code null} 时不发送，无有效元素时发送空字符串
         * @return 当前构造器
         */
        public Builder cycleMonth(Collection<String> cycleMonth) {
            this.cycleMonth = TaskTemplateRequestValidators.joinOptional(cycleMonth);
            return this;
        }

        /**
         * 设置周期月份逗号字符串。
         *
         * <p>用于调用方已有 wire 字符串的场景，发送时不再拆分校验范围。</p>
         *
         * @param cycleMonth 英文逗号分隔的周期月份编码；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder cycleMonth(String cycleMonth) {
            this.cycleMonth = cycleMonth;
            return this;
        }

        /**
         * 设置周期星期集合，发送时会拼成英文逗号字符串。
         *
         * <p>星期编码按平台规则透传，SDK 不做自然周含义转换。</p>
         *
         * @param cycleWeek 周期星期编码集合；{@code null} 时不发送，无有效元素时发送空字符串
         * @return 当前构造器
         */
        public Builder cycleWeek(Collection<String> cycleWeek) {
            this.cycleWeek = TaskTemplateRequestValidators.joinOptional(cycleWeek);
            return this;
        }

        /**
         * 设置周期星期逗号字符串。
         *
         * <p>用于调用方已有 wire 字符串的场景，SDK 不把它转换成 JSON 数组。</p>
         *
         * @param cycleWeek 英文逗号分隔的周期星期编码；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder cycleWeek(String cycleWeek) {
            this.cycleWeek = cycleWeek;
            return this;
        }

        /**
         * 设置周期任务每日执行时间，发送格式为 {@code HH:mm:ss}。
         *
         * @param cycleExecuteTime 周期任务每日执行时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder cycleExecuteTime(LocalTime cycleExecuteTime) {
            this.cycleExecuteTime = cycleExecuteTime;
            return this;
        }

        /**
         * 设置周期任务生效开始时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
         *
         * @param cycleStartTime 周期任务生效开始时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder cycleStartTime(LocalDateTime cycleStartTime) {
            this.cycleStartTime = cycleStartTime;
            return this;
        }

        /**
         * 设置周期任务生效结束时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
         *
         * @param cycleEndTime 周期任务生效结束时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder cycleEndTime(LocalDateTime cycleEndTime) {
            this.cycleEndTime = cycleEndTime;
            return this;
        }

        /**
         * 设置间隔执行数量。
         *
         * <p>例如每 2 小时执行一次时，本字段为 {@code 2}，单位由 {@link #intervalType(Integer)} 决定。</p>
         *
         * @param intervalNumber 间隔数量；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder intervalNumber(Integer intervalNumber) {
            this.intervalNumber = intervalNumber;
            return this;
        }

        /**
         * 设置间隔执行单位类型，按平台编码透传，例如 1=小时、2=天、3=分钟。
         *
         * @param intervalType 平台支持的间隔单位编码；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder intervalType(Integer intervalType) {
            this.intervalType = intervalType;
            return this;
        }

        /**
         * 设置间隔任务参考执行时间，发送格式为 {@code HH:mm:ss}。
         *
         * <p>该字段由平台用于计算间隔计划展示和首个执行时间，SDK 不做时间区间推导。</p>
         *
         * @param intervalExecuteTime 间隔任务参考执行时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder intervalExecuteTime(LocalTime intervalExecuteTime) {
            this.intervalExecuteTime = intervalExecuteTime;
            return this;
        }

        /**
         * 设置间隔任务生效开始时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
         *
         * @param intervalStartTime 间隔任务生效开始时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder intervalStartTime(LocalDateTime intervalStartTime) {
            this.intervalStartTime = intervalStartTime;
            return this;
        }

        /**
         * 设置间隔任务生效结束时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
         *
         * @param intervalEndTime 间隔任务生效结束时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder intervalEndTime(LocalDateTime intervalEndTime) {
            this.intervalEndTime = intervalEndTime;
            return this;
        }

        /**
         * 设置模板整体不可用开始时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
         *
         * @param invalidStartTime 模板整体不可用开始时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder invalidStartTime(LocalDateTime invalidStartTime) {
            this.invalidStartTime = invalidStartTime;
            return this;
        }

        /**
         * 设置模板整体不可用结束时间，发送格式为 {@code yyyy-MM-dd HH:mm:ss}。
         *
         * @param invalidEndTime 模板整体不可用结束时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder invalidEndTime(LocalDateTime invalidEndTime) {
            this.invalidEndTime = invalidEndTime;
            return this;
        }

        /**
         * 设置每日不可用开始时间，发送格式为 {@code HH:mm:ss}。
         *
         * @param invalidDailyStartTime 每日不可用开始时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder invalidDailyStartTime(LocalTime invalidDailyStartTime) {
            this.invalidDailyStartTime = invalidDailyStartTime;
            return this;
        }

        /**
         * 设置每日不可用结束时间，发送格式为 {@code HH:mm:ss}。
         *
         * @param invalidDailyEndTime 每日不可用结束时间；{@code null} 时不发送
         * @return 当前构造器
         */
        public Builder invalidDailyEndTime(LocalTime invalidDailyEndTime) {
            this.invalidDailyEndTime = invalidDailyEndTime;
            return this;
        }

        /**
         * 构建不可变新增模板请求。
         *
         * <p>必填字段和格式化规则在 {@link TaskTemplateCreateRequest#toMap()} 中统一执行，
         * 因此 builder 可以作为纯数据收集器使用。</p>
         *
         * @return 不可变任务模板新增请求
         */
        public TaskTemplateCreateRequest build() {
            return new TaskTemplateCreateRequest(this);
        }
    }
}
