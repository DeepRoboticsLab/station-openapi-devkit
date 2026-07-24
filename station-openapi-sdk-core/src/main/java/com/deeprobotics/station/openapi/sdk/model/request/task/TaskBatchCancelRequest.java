package com.deeprobotics.station.openapi.sdk.model.request.task;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 批量取消任务请求。
 *
 * <p>对应 {@code POST /remoteApi/batchCancelTask}。
 * {@code taskExecuteCodeList} 的 wire 类型必须保持 JSON 数组。</p>
 */
public final class TaskBatchCancelRequest implements OpenApiRequest {

    /**
     * 待取消的任务执行编码列表。
     *
     * <p>wire 字段名固定为 {@code taskExecuteCodeList}，类型是字符串数组。该字段必须发送 JSON 数组，
     * 不能改成逗号字符串；逗号字符串只用于 {@code statusArr} 等 wire 类型为 String 的字段。</p>
     */
    private final List<String> taskExecuteCodeList;

    private TaskBatchCancelRequest(Builder builder) {
        this.taskExecuteCodeList = builder.taskExecuteCodeList;
    }

    /**
     * 创建批量取消任务请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        /*
         * 本地过滤 null、空白元素并保留为 List，保证 Jackson 序列化后仍是 JSON array。
         * 批量取消是 TASK_CONTROL，执行器不会默认重试，因此不能在这里做任何拆分或循环重发。
         */
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("taskExecuteCodeList", TaskRequestValidators.requireStringList("taskExecuteCodeList",
                taskExecuteCodeList));
        return body;
    }

    /**
     * 构造批量取消任务请求。
     */
    public static final class Builder {
        /**
         * 客户传入的任务执行编码集合副本。
         *
         * <p>setter 会复制集合，避免调用方在请求对象创建后继续修改原集合，导致签名源和实际请求不一致。</p>
         */
        private List<String> taskExecuteCodeList;

        private Builder() {
        }

        /**
         * 设置待取消的任务执行编码集合。
         *
         * @param taskExecuteCodeList 任务执行编码集合，不能为空；元素会在发送前 trim 并过滤空白值
         * @return 当前 builder
         */
        public Builder taskExecuteCodeList(Collection<String> taskExecuteCodeList) {
            this.taskExecuteCodeList = taskExecuteCodeList == null ? null : new ArrayList<>(taskExecuteCodeList);
            return this;
        }

        /**
         * 创建不可变批量取消请求。集合有效性会在调用接口时统一校验。
         *
         * @return 不可变批量取消请求
         */
        public TaskBatchCancelRequest build() {
            return new TaskBatchCancelRequest(this);
        }
    }
}
