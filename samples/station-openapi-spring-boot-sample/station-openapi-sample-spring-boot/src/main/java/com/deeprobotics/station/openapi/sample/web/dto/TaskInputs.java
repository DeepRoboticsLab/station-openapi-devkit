package com.deeprobotics.station.openapi.sample.web.dto;

import com.deeprobotics.station.openapi.sdk.model.request.task.TaskBatchCancelRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskIssueRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskPageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

/**
 * 任务下发、控制、取消与查询接口的轻量 Web 入参集合。
 *
 * <p>record 负责 HTTP 结构校验并显式构建 SDK 请求；批量任务编码保持 JSON 数组，
 * 查询状态集合的 wire 格式转换由 SDK 统一完成。</p>
 */
public final class TaskInputs {

    private TaskInputs() {
    }

    public record Issue(
            @NotBlank(message = "REMOTE_API_TASK_ISSUE_TASK_CODE_REQUIRED") String taskCode) {

        public TaskIssueRequest toSdkRequest() {
            return TaskIssueRequest.builder().taskCode(taskCode).build();
        }
    }

    public record Control(
            @NotBlank(message = "REMOTE_API_TASK_EXECUTE_CODE_REQUIRED") String taskExecuteCode,
            @Schema(description = "控制命令：当前 2=暂停、3=恢复、4=终止；也可透传平台新增编码 / "
                    + "Control command: currently 2=pause, 3=resume, 4=stop; new platform codes are accepted")
            @NotBlank(message = "REMOTE_API_TASK_CTRL_COMMAND_REQUIRED") String command) {

        public TaskControlRequest toSdkRequest() {
            return TaskControlRequest.builder()
                    .taskExecuteCode(taskExecuteCode)
                    .command(command)
                    .build();
        }
    }

    public record BatchCancel(
            @NotEmpty(message = "REMOTE_API_TASK_EXECUTE_CODE_LIST_REQUIRED")
            List<String> taskExecuteCodeList) {

        /**
         * 构建 SDK 请求；{@code taskExecuteCodeList} 保持数组语义，不在 Sample 中拼接字符串。
         */
        public TaskBatchCancelRequest toSdkRequest() {
            return TaskBatchCancelRequest.builder()
                    .taskExecuteCodeList(taskExecuteCodeList)
                    .build();
        }
    }

    public record Page(
            String dogCode,
            String taskName,
            List<String> statusArr,
            Integer sendStatus,
            Integer priority,
            String status,
            Boolean showUnExecuted,
            LocalDate patrolStartTime,
            LocalDate patrolEndTime,
            Integer pageNo,
            Integer pageSize) {

        /**
         * 构建 SDK 请求；状态集合的逗号串 wire 语义和分页归一化均由 SDK 处理。
         */
        public TaskPageRequest toSdkRequest() {
            return TaskPageRequest.builder()
                    .dogCode(dogCode)
                    .taskName(taskName)
                    .statusArr(statusArr)
                    .sendStatus(sendStatus)
                    .priority(priority)
                    .status(status)
                    .showUnExecuted(showUnExecuted)
                    .patrolStartTime(patrolStartTime)
                    .patrolEndTime(patrolEndTime)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .build();
        }
    }
}
