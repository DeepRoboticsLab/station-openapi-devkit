package com.deeprobotics.station.openapi.sdk.model.request.tasktemplate;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.PageRequestSupport;
import java.util.LinkedHashMap;

/**
 * 任务模板分页查询请求。
 *
 * <p>对应 {@code POST /remoteApi/modelPage}。所有查询条件都可选；
 * SDK 仍会始终发送归一后的 {@code pageNo/pageSize}，并且永远不会输出历史 {@code pageNum} 字段。</p>
 */
public final class TaskTemplatePageRequest extends PageRequestSupport {

    /**
     * 任务模板名称过滤条件，平台使用模糊匹配。
     */
    private final String taskName;
    /**
     * 巡检类型过滤条件，按平台字符串编码透传。
     */
    private final String patrolType;
    /**
     * 四足狗编码过滤条件，用于筛选绑定或适用指定设备的模板。
     */
    private final String dogCode;
    /**
     * 优先级过滤条件。wire 类型为字符串；{@code "1"} 最低、{@code "4"} 最高。
     */
    private final String priority;
    /**
     * 是否启用过滤条件：{@code 1} 启用、{@code 0} 停用。
     */
    private final Integer isEnable;
    /**
     * SDK 对外分页页码，只使用 pageNo，不使用历史 pageNum。
     */
    private final Integer pageNo;
    /**
     * SDK 对外分页大小，发送前由 {@link PageRequestSupport} 归一到默认值和最大值。
     */
    private final Integer pageSize;

    private TaskTemplatePageRequest(Builder builder) {
        this.taskName = builder.taskName;
        this.patrolType = builder.patrolType;
        this.dogCode = builder.dogCode;
        this.priority = builder.priority;
        this.isEnable = builder.isEnable;
        this.pageNo = builder.pageNo;
        this.pageSize = builder.pageSize;
    }

    /**
     * 创建任务模板分页请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        /*
         * 查询条件全部可选，null 不进入请求体；分页字段始终通过 putPage 输出归一后的 pageNo/pageSize。
         * 这能保证生产代码不会发出历史 pageNum 字段。
         */
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        MapUtils.putIfNotNull(body, "taskName", taskName);
        MapUtils.putIfNotNull(body, "patrolType", patrolType);
        MapUtils.putIfNotNull(body, "dogCode", dogCode);
        MapUtils.putIfNotNull(body, "priority", priority);
        MapUtils.putIfNotNull(body, "isEnable", isEnable);
        putPage(body, pageNo, pageSize);
        return body;
    }

    /**
     * 收集任务模板过滤条件和分页参数并创建不可变请求。
     */
    public static final class Builder {
        private String taskName;
        private String patrolType;
        private String dogCode;
        private String priority;
        private Integer isEnable;
        private Integer pageNo;
        private Integer pageSize;

        private Builder() {
        }

        /**
         * 设置任务模板名称查询条件。
         *
         * @param taskName 任务模板名称关键字；{@code null} 时不发送该过滤条件
         * @return 当前构造器
         */
        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        /**
         * 设置巡检类型查询条件，按平台字符串编码透传。
         *
         * @param patrolType 平台支持的巡检类型编码；{@code null} 时不发送该过滤条件
         * @return 当前构造器
         */
        public Builder patrolType(String patrolType) {
            this.patrolType = patrolType;
            return this;
        }

        /**
         * 设置四足狗编码查询条件。
         *
         * @param dogCode 四足狗编码；{@code null} 时不发送该过滤条件
         * @return 当前构造器
         */
        public Builder dogCode(String dogCode) {
            this.dogCode = dogCode;
            return this;
        }

        /**
         * 设置优先级查询条件。
         *
         * <p>这里保持字符串类型以匹配 wire contract，避免调用方已有编码被 SDK 强制转换。</p>
         *
         * @param priority 平台支持的优先级原始编码；{@code null} 时不发送该过滤条件
         * @return 当前构造器
         */
        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * 设置启用状态查询条件：{@code 1} 启用、{@code 0} 停用。
         *
         * @param isEnable 启用状态编码；{@code null} 时不发送该过滤条件
         * @return 当前构造器
         */
        public Builder isEnable(Integer isEnable) {
            this.isEnable = isEnable;
            return this;
        }

        /**
         * 设置页码。
         *
         * <p>不传时默认第 1 页；小于 1 会在发送前抛本地参数异常。</p>
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
         * <p>不传或小于等于 0 时归一为 10；大于 100 时归一为 100，避免调用方一次拉取过大数据量。</p>
         *
         * @param pageSize 每页数量
         * @return 当前构造器
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * 构建不可变分页请求。
         *
         * @return 不可变任务模板分页请求
         */
        public TaskTemplatePageRequest build() {
            return new TaskTemplatePageRequest(this);
        }
    }
}
