package com.deeprobotics.station.openapi.sdk.model.request.result;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.PageRequestSupport;
import java.util.LinkedHashMap;

/**
 * 任务巡检点结果分页查询请求。
 *
 * <p>对应 {@code POST /remoteApi/taskPointResultPage}。{@code taskExecuteCode} 必填，分页字段统一使用
 * SDK 公共 {@code pageNo/pageSize} 规则。</p>
 */
public final class TaskPointResultPageRequest extends PageRequestSupport {

    /**
     * 任务执行编码。
     *
     * <p>该值来自任务下发返回值或任务分页结果，用于定位一次具体任务执行下的巡检点结果。
     * 平台按该字段查询点位识别值、告警、文件列表等结果数据。</p>
     */
    private final String taskExecuteCode;
    /**
     * 页码，从 1 开始。
     *
     * <p>SDK 只发送 {@code pageNo}，不会发送历史 {@code pageNum}。</p>
     */
    private final Integer pageNo;
    /**
     * 每页数量。
     *
     * <p>沿用 SDK 公共分页规则：null 或非正数为 10，大于 100 截断为 100。</p>
     */
    private final Integer pageSize;

    private TaskPointResultPageRequest(Builder builder) {
        this.taskExecuteCode = builder.taskExecuteCode;
        this.pageNo = builder.pageNo;
        this.pageSize = builder.pageSize;
    }

    /**
     * 创建巡检点结果分页请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        /*
         * 结果分页是普通 JSON READ_ONLY 接口。taskExecuteCode 必填，分页字段由公共基类统一归一，
         * 这样客户在任务分页和结果分页之间切换时看到一致的 pageNo/pageSize 规则。
         */
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("taskExecuteCode", ResultRequestValidators.requireText("taskExecuteCode", taskExecuteCode));
        putPage(body, pageNo, pageSize);
        return body;
    }

    /**
     * 构造巡检点结果分页请求。
     */
    public static final class Builder {
        /**
         * 客户传入的任务执行编码，发送前会 trim 并校验非空。
         */
        private String taskExecuteCode;
        /**
         * 页码，从 1 开始；null 使用默认值。
         */
        private Integer pageNo;
        /**
         * 分页大小；null/非正数/超过上限会被公共分页逻辑归一。
         */
        private Integer pageSize;

        private Builder() {
        }

        /**
         * 设置任务执行编码。
         *
         * @param taskExecuteCode 一次任务执行编码，来自任务下发或任务分页接口
         * @return 当前 builder
         */
        public Builder taskExecuteCode(String taskExecuteCode) {
            this.taskExecuteCode = taskExecuteCode;
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
         * 设置每页数量；{@code null} 或非正数使用 10，大于 100 截断为 100。
         *
         * @param pageSize 每页数量
         * @return 当前构造器
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * 创建不可变结果分页请求。必填和分页校验会在接口调用时执行。
         *
         * @return 不可变巡检点结果分页请求
         */
        public TaskPointResultPageRequest build() {
            return new TaskPointResultPageRequest(this);
        }
    }
}
