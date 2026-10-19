package com.deeprobotics.station.openapi.sdk.model.request.tasktemplate;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 切换任务模板启用状态的请求，对应平台 v1.3.3 起提供的
 * {@code POST /remoteApi/switchTaskModelStatus}。
 *
 * <p>停用会清除该模板关联的未执行任务；启用只允许模板再次下发，仍需另行调用任务下发接口。
 * 本请求的 {@code isEnable} 是整数，与模板创建和详情中的同名字符串字段不同。</p>
 */
public final class TaskTemplateStatusRequest implements OpenApiRequest {

    private final String taskCode;
    private final Integer isEnable;

    private TaskTemplateStatusRequest(Builder builder) {
        this.taskCode = builder.taskCode;
        this.isEnable = builder.isEnable;
    }

    /**
     * 创建启停请求构造器；不设置默认状态，避免遗漏参数导致意外停用。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 校验参数并生成确定顺序的业务请求体；鉴权由 SDK 执行器添加。
     *
     * @return 含 taskCode 和整数 isEnable 的请求体
     * @throws StationOpenApiClientException 编码为空、状态缺失或不是 0/1
     */
    @Override
    public LinkedHashMap<String, Object> toMap() {
        String requiredTaskCode = TaskTemplateRequestValidators.requireText("taskCode", taskCode);
        if (isEnable == null || (isEnable != 0 && isEnable != 1)) {
            throw new StationOpenApiClientException("isEnable must be 0 or 1");
        }
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("taskCode", requiredTaskCode);
        body.put("isEnable", isEnable);
        return body;
    }

    /** 收集任务模板编码及目标状态，构建不可变请求。 */
    public static final class Builder {
        private String taskCode;
        private Integer isEnable;

        private Builder() {
        }

        /**
         * 设置模板编码；发送前去除首尾空白。
         *
         * @param taskCode 模板创建、详情或分页返回的 taskCode，必填
         * @return 当前构造器
         */
        public Builder taskCode(String taskCode) {
            this.taskCode = taskCode;
            return this;
        }

        /**
         * 设置目标启用状态。
         *
         * @param isEnable 0 停用、1 启用；必须显式设置
         * @return 当前构造器
         */
        public Builder isEnable(Integer isEnable) {
            this.isEnable = isEnable;
            return this;
        }

        /**
         * 构建请求；参数在生成请求体、发起网络调用之前校验。
         *
         * @return 不可变启停请求
         */
        public TaskTemplateStatusRequest build() {
            return new TaskTemplateStatusRequest(this);
        }
    }
}
