package com.deeprobotics.station.openapi.sdk.model.request.task;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 任务模板下发请求。
 *
 * <p>对应 {@code POST /remoteApi/issue}，成功后返回
 * 新生成的任务执行编码 {@code taskExecuteCode}。</p>
 */
public final class TaskIssueRequest implements OpenApiRequest {

    /**
     * 任务模板编码。
     *
     * <p>该值来自任务模板创建接口返回值，或模板详情/分页结果中的 {@code taskCode}。下发接口会基于这个模板
     * 生成新的任务执行记录，因此这里传的是“模板编码”，不是一次执行的 {@code taskExecuteCode}。</p>
     */
    private final String taskCode;

    private TaskIssueRequest(Builder builder) {
        this.taskCode = builder.taskCode;
    }

    /**
     * 创建任务下发请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        /*
         * toMap() 是签名源和 JSON 请求体的共同来源，字段顺序需要稳定。
         * taskCode 是平台必填字段，本地先 trim 并校验，避免无效请求进入鉴权和 HTTP 层。
         */
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("taskCode", TaskRequestValidators.requireText("taskCode", taskCode));
        return body;
    }

    /**
     * 构造任务下发请求。
     *
     * <p>Builder 只暂存客户输入，不在 setter 中抛异常；校验统一放在 {@link #toMap()}，
     * 这样普通 JSON 调用和下载/重试链路都能共享同一套校验结果。</p>
     */
    public static final class Builder {
        /**
         * 客户输入的任务模板编码，允许包含首尾空格，最终发送前会 trim。
         */
        private String taskCode;

        private Builder() {
        }

        /**
         * 设置要下发的任务模板编码。
         *
         * @param taskCode 任务模板编码，最终发送前会去除首尾空格，空白值会在调用时失败
         * @return 当前 builder，便于链式调用
         */
        public Builder taskCode(String taskCode) {
            this.taskCode = taskCode;
            return this;
        }

        /**
         * 创建不可变请求对象。
         *
         * <p>这里不触发校验，避免仅构造对象就抛异常；真正调用接口时由 {@code toMap()} 校验。</p>
         *
         * @return 不可变任务下发请求
         */
        public TaskIssueRequest build() {
            return new TaskIssueRequest(this);
        }
    }
}
