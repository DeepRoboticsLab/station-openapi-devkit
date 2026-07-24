package com.deeprobotics.station.openapi.sdk.model.request.tasktemplate;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 任务模板详情查询请求。
 *
 * <p>对应 {@code POST /remoteApi/taskModelByCode}。</p>
 */
public final class TaskTemplateGetRequest implements OpenApiRequest {

    /**
     * 任务模板编码，必填。
     *
     * <p>通常来自创建接口返回的 {@code taskCode}，或模板分页/详情列表中的同名字段。</p>
     */
    private final String taskCode;

    private TaskTemplateGetRequest(Builder builder) {
        this.taskCode = builder.taskCode;
    }

    /**
     * 创建任务模板详情请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        // 详情查询只有 taskCode 一个业务字段，鉴权字段由执行器统一补充。
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("taskCode", TaskTemplateRequestValidators.requireText("taskCode", taskCode));
        return body;
    }

    /**
     * 收集任务模板编码并创建不可变详情请求。
     */
    public static final class Builder {
        private String taskCode;

        private Builder() {
        }

        /**
         * 设置任务模板编码。
         *
         * <p>SDK 发送前会 trim 前后空白；编码值本身按平台生成结果原样透传。</p>
         *
         * @param taskCode 任务模板编码
         * @return 当前构造器
         */
        public Builder taskCode(String taskCode) {
            this.taskCode = taskCode;
            return this;
        }

        /**
         * 构建不可变详情查询请求。
         *
         * @return 不可变任务模板详情请求
         */
        public TaskTemplateGetRequest build() {
            return new TaskTemplateGetRequest(this);
        }
    }
}
