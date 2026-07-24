package com.deeprobotics.station.openapi.sdk.model.request.task;

import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import java.util.LinkedHashMap;

/**
 * 任务执行控制请求。
 *
 * <p>对应 {@code POST /remoteApi/taskCtrl}。</p>
 */
public final class TaskControlRequest implements OpenApiRequest {

    /**
     * 任务执行编码。
     *
     * <p>该值来自 {@code task().issue(...)} 返回值或 {@code task().page(...)} 的分页结果。
     * 任务控制针对的是一次具体执行实例，不能使用任务模板的 {@code taskCode} 代替。</p>
     */
    private final String taskExecuteCode;
    /**
     * 控制命令 wire code。
     *
     * <p>平台字段名固定为 {@code command}，类型是字符串。SDK 建议调用方使用
     * {@link TaskControlCommand} 常量，避免在业务代码里散落 {@code "2"}/{@code "3"}/{@code "4"}。</p>
     */
    private final String command;

    private TaskControlRequest(Builder builder) {
        this.taskExecuteCode = builder.taskExecuteCode;
        this.command = builder.command;
    }

    /**
     * 创建任务控制请求构造器。
     *
     * @return 新的请求构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public LinkedHashMap<String, Object> toMap() {
        /*
         * 控制接口属于 TASK_CONTROL，执行器默认不重试。这里仍然只负责构造确定性的 wire body：
         * 先写 taskExecuteCode，再写 command，确保签名源、测试断言和问题排查看到同样的字段顺序。
         */
        LinkedHashMap<String, Object> body = MapUtils.newBody();
        body.put("taskExecuteCode", TaskRequestValidators.requireText("taskExecuteCode", taskExecuteCode));
        body.put("command", TaskRequestValidators.requireText("command", command));
        return body;
    }

    /**
     * 构造任务控制请求。
     */
    public static final class Builder {
        /**
         * 客户输入的任务执行编码，最终发送前会 trim 并校验非空。
         */
        private String taskExecuteCode;
        /**
         * 已归一为平台字符串编码的控制命令。
         */
        private String command;

        private Builder() {
        }

        /**
         * 设置任务执行编码。
         *
         * @param taskExecuteCode 一次任务执行的编码，来自任务下发或任务分页结果
         * @return 当前 builder
         */
        public Builder taskExecuteCode(String taskExecuteCode) {
            this.taskExecuteCode = taskExecuteCode;
            return this;
        }

        /**
         * 使用 SDK 命令对象设置控制指令。
         *
         * <p>这是推荐入口，可读性比直接传字符串更好；如果平台增加新命令，
         * 可先用 {@link TaskControlCommand#rawCode(String)} 生成命令对象。</p>
         *
         * @param command SDK 任务控制命令；{@code null} 会在接口调用时按缺少必填参数处理
         * @return 当前构造器
         */
        public Builder command(TaskControlCommand command) {
            this.command = command == null ? null : command.code();
            return this;
        }

        /**
         * 直接设置平台命令字符串。
         *
         * <p>保留该入口是为了兼容平台新增但 SDK 尚未发布常量的命令编码。调用方应优先使用
         * {@link #command(TaskControlCommand)}，只有在明确知道平台编码时才使用 raw string。</p>
         *
         * @param command 平台支持的任务控制命令原始编码
         * @return 当前构造器
         */
        public Builder command(String command) {
            this.command = command;
            return this;
        }

        /**
         * 创建不可变任务控制请求。字段有效性会在调用接口时统一校验。
         *
         * @return 不可变任务控制请求
         */
        public TaskControlRequest build() {
            return new TaskControlRequest(this);
        }
    }
}
