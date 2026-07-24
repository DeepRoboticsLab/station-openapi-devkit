package com.deeprobotics.station.openapi.sample.web.dto;

import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateCreateRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateDeleteRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateGetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplatePageRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 任务模板新增、删除与查询接口的轻量 Web 入参集合。
 *
 * <p>record 负责 HTTP 结构校验并显式构建 SDK 请求；点位、月份和星期等集合保持 Java 集合，
 * 由 SDK 按平台 wire contract 转换为逗号串。</p>
 */
public final class TaskTemplateInputs {

    private TaskTemplateInputs() {
    }

    public record Create(
            @NotBlank(message = "REMOTE_API_TASK_NAME_REQUIRED") String taskName,
            Integer executeType,
            String patrolType,
            @NotEmpty(message = "REMOTE_API_TASK_POINT_CODES_REQUIRED") List<String> pointCodes,
            Integer priority,
            @NotBlank(message = "REMOTE_API_TASK_END_ACTION_REQUIRED") String endAction,
            Integer deviceLevel,
            String isEnable,
            LocalDateTime fixedStartTime,
            List<String> cycleMonth,
            List<String> cycleWeek,
            LocalTime cycleExecuteTime,
            LocalDateTime cycleStartTime,
            LocalDateTime cycleEndTime,
            Integer intervalNumber,
            Integer intervalType,
            LocalTime intervalExecuteTime,
            LocalDateTime intervalStartTime,
            LocalDateTime intervalEndTime,
            LocalDateTime invalidStartTime,
            LocalDateTime invalidEndTime,
            LocalTime invalidDailyStartTime,
            LocalTime invalidDailyEndTime) {

        /**
         * 构建 SDK 请求；集合字段不在 Sample 中预先拼接，由 SDK 统一转换 wire 格式。
         */
        public TaskTemplateCreateRequest toSdkRequest() {
            return TaskTemplateCreateRequest.builder()
                    .taskName(taskName)
                    .executeType(executeType)
                    .patrolType(patrolType)
                    .pointCodes(pointCodes)
                    .priority(priority)
                    .endAction(endAction)
                    .deviceLevel(deviceLevel)
                    .isEnable(isEnable)
                    .fixedStartTime(fixedStartTime)
                    .cycleMonth(cycleMonth)
                    .cycleWeek(cycleWeek)
                    .cycleExecuteTime(cycleExecuteTime)
                    .cycleStartTime(cycleStartTime)
                    .cycleEndTime(cycleEndTime)
                    .intervalNumber(intervalNumber)
                    .intervalType(intervalType)
                    .intervalExecuteTime(intervalExecuteTime)
                    .intervalStartTime(intervalStartTime)
                    .intervalEndTime(intervalEndTime)
                    .invalidStartTime(invalidStartTime)
                    .invalidEndTime(invalidEndTime)
                    .invalidDailyStartTime(invalidDailyStartTime)
                    .invalidDailyEndTime(invalidDailyEndTime)
                    .build();
        }
    }

    public record Delete(
            @NotEmpty(message = "REMOTE_API_TASK_CODES_REQUIRED") List<String> taskCodes) {

        /**
         * 构建 SDK 请求；{@code taskCodes} 保持 JSON 数组语义，不转换为逗号串。
         */
        public TaskTemplateDeleteRequest toSdkRequest() {
            return TaskTemplateDeleteRequest.builder().taskCodes(taskCodes).build();
        }
    }

    public record GetByCode(
            @NotBlank(message = "REMOTE_API_TASK_CODE_REQUIRED") String taskCode) {

        public TaskTemplateGetRequest toSdkRequest() {
            return TaskTemplateGetRequest.builder().taskCode(taskCode).build();
        }
    }

    public record Page(
            String taskName,
            String patrolType,
            String dogCode,
            String priority,
            Integer isEnable,
            Integer pageNo,
            Integer pageSize) {

        public TaskTemplatePageRequest toSdkRequest() {
            return TaskTemplatePageRequest.builder()
                    .taskName(taskName)
                    .patrolType(patrolType)
                    .dogCode(dogCode)
                    .priority(priority)
                    .isEnable(isEnable)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .build();
        }
    }
}
