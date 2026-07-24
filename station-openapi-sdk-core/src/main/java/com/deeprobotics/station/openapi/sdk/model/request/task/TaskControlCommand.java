package com.deeprobotics.station.openapi.sdk.model.request.task;

import java.util.Objects;

/**
 * 任务控制命令。
 *
 * <p>平台 wire 字段 {@code command} 当前使用字符串编码：2=暂停、3=恢复、4=停止/终止。
 * {@link #rawCode(String)} 用于透传平台新增编码。</p>
 */
public final class TaskControlCommand {

    /**
     * 暂停任务执行，平台 wire code 为 {@code "2"}。
     */
    public static final TaskControlCommand PAUSE = new TaskControlCommand("2");
    /**
     * 恢复已暂停的任务执行，平台 wire code 为 {@code "3"}。
     */
    public static final TaskControlCommand RESUME = new TaskControlCommand("3");
    /**
     * 停止/终止任务执行，平台 wire code 为 {@code "4"}。
     */
    public static final TaskControlCommand STOP = new TaskControlCommand("4");

    /**
     * 最终发送到平台 {@code command} 字段的字符串编码。
     */
    private final String code;

    private TaskControlCommand(String code) {
        this.code = TaskRequestValidators.requireText("command", code);
    }

    /**
     * 创建自定义命令编码。
     *
     * <p>平台可能先于 SDK 发布新增控制命令。该方法让调用方在升级 SDK 之前仍可透传新编码，
     * 同时继续复用 SDK 的必填校验、签名、异常和重试策略。</p>
     *
     * @param code 平台认可的命令编码，不能为空白
     * @return 自定义任务控制命令
     */
    public static TaskControlCommand rawCode(String code) {
        return new TaskControlCommand(code);
    }

    /**
     * 包内访问的 wire code。
     *
     * <p>只给请求 Builder 使用，避免公共 API 暴露过多内部命名；客户需要展示时可使用 {@link #toString()}。</p>
     */
    String code() {
        return code;
    }

    @Override
    public String toString() {
        // 命令码本身不是敏感信息，直接返回便于日志和断言展示。
        return code;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TaskControlCommand command)) {
            return false;
        }
        return Objects.equals(code, command.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
