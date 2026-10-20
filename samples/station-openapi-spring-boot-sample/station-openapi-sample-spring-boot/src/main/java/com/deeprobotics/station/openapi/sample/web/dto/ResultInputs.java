package com.deeprobotics.station.openapi.sample.web.dto;

import com.deeprobotics.station.openapi.sdk.model.request.result.ResourceDownloadRequest;
import com.deeprobotics.station.openapi.sdk.model.request.result.TaskPointResultPageRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 巡检结果查询与资源下载接口的轻量 Web 入参集合。
 *
 * <p>record 校验必填字段和资源类型，再构建 SDK 请求；资源路径应使用平台返回值。</p>
 */
public final class ResultInputs {

    private ResultInputs() {
    }

    public record PageTaskPointResults(
            @NotBlank(message = "REMOTE_API_TASK_RESULT_EXECUTE_CODE_REQUIRED") String taskExecuteCode,
            Integer pageNo,
            Integer pageSize) {

        public TaskPointResultPageRequest toSdkRequest() {
            return TaskPointResultPageRequest.builder()
                    .taskExecuteCode(taskExecuteCode)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .build();
        }
    }

    public record DownloadResource(
            @NotBlank(message = "REMOTE_API_RESOURCE_FILE_PATH_REQUIRED") String filePath,
            @NotNull(message = "REMOTE_API_RESOURCE_TYPE_REQUIRED")
            @Min(value = 1, message = "REMOTE_API_RESOURCE_TYPE_RANGE")
            @Max(value = 2, message = "REMOTE_API_RESOURCE_TYPE_RANGE") Integer type) {

        /**
         * 构建 SDK 下载请求；SDK 校验路径非空及资源类型为 1 或 2。
         */
        public ResourceDownloadRequest toSdkRequest() {
            return ResourceDownloadRequest.builder()
                    .filePath(filePath)
                    .type(type)
                    .build();
        }
    }
}
