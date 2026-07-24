package com.deeprobotics.station.openapi.sdk.model.request.tasktemplate;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 删除任务模板请求。
 *
 * <p>对应 {@code POST /remoteApi/removeTaskModelByCodes}。注意
 * {@code taskCodes} 的 wire 类型是 JSON 数组，不能转换成逗号字符串。</p>
 */
public final class TaskTemplateDeleteRequest implements OpenApiRequest {

    /**
     * 需要删除的任务模板编码列表。
     *
     * <p>wire 类型必须保持 JSON 数组，与 {@code pointCodes/cycleMonth/cycleWeek} 这类逗号字符串字段不同。</p>
     */
    private final List<String> taskCodes;

    private TaskTemplateDeleteRequest(Builder builder) {
        this.taskCodes = builder.taskCodes;
    }

    /**
     * 创建任务模板删除请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        // 删除模板是业务写操作，发送前先过滤空编码，避免平台收到无效数组后产生部分成功/失败的不确定状态。
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("taskCodes", TaskTemplateRequestValidators.requireStringList("taskCodes", taskCodes));
        return body;
    }

    /**
     * 收集待删除模板编码并创建不可变请求。
     */
    public static final class Builder {
        private List<String> taskCodes;

        private Builder() {
        }

        /**
         * 设置需要删除的任务模板编码列表。
         *
         * <p>builder 会复制传入集合，避免调用方后续修改原集合影响已构建请求。</p>
         *
         * @param taskCodes 任务模板编码集合；发送前会过滤空白元素，最终必须至少包含一个有效编码
         * @return 当前构造器
         */
        public Builder taskCodes(Collection<String> taskCodes) {
            this.taskCodes = taskCodes == null ? null : new ArrayList<>(taskCodes);
            return this;
        }

        /**
         * 构建不可变删除请求。
         *
         * @return 不可变任务模板删除请求
         */
        public TaskTemplateDeleteRequest build() {
            return new TaskTemplateDeleteRequest(this);
        }
    }
}
