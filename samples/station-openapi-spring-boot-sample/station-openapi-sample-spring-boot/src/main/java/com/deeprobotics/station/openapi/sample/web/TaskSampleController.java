package com.deeprobotics.station.openapi.sample.web;

import com.deeprobotics.station.openapi.sample.safety.DangerousSampleOperation;
import com.deeprobotics.station.openapi.sample.service.TaskSampleService;
import com.deeprobotics.station.openapi.sample.web.dto.TaskInputs;
import com.deeprobotics.station.openapi.sample.web.view.PageView;
import com.deeprobotics.station.openapi.sample.web.view.ValueResponse;
import com.deeprobotics.station.openapi.sdk.model.response.task.TaskExecuteInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务下发、控制和查询的薄 Web 示例，对应 SDK 的 {@code client.task()} 分组。
 *
 * <p>Controller 只处理 HTTP 语义和危险操作标记，不在 Sample 中实现任务状态机或自动重试。</p>
 */
@RestController
@RequestMapping("/sample/api/task")
@Tag(name = "Task", description = "任务执行 / Task execution")
@ApiResponse(
        responseCode = "400",
        description = "参数校验失败 / Validation failed",
        content = @Content(mediaType = "application/problem+json", examples = {
                @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.VALIDATION_ZH),
                @ExampleObject(name = "en_US", value = SampleOpenApiExamples.VALIDATION_EN)
        }))
public class TaskSampleController {

    private final TaskSampleService service;

    public TaskSampleController(TaskSampleService service) {
        this.service = service;
    }

    @PostMapping("/issue")
    @DangerousSampleOperation
    @Operation(
            operationId = "task.issue",
            summary = "下发任务 / Issue task",
            description = "危险操作：会将任务下发到现场设备。"
                    + " Dangerous operation: issues a task to a physical device.",
            responses = @ApiResponse(
                    responseCode = "403",
                    description = "危险操作默认关闭 / Dangerous operation disabled by default",
                    content = @Content(mediaType = "application/problem+json", examples = {
                            @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                            @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                    })))
    public ValueResponse issue(
            @Valid @RequestBody TaskInputs.Issue input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String taskExecuteCode = service.issue(input.toSdkRequest(), SampleRequestOptions.from(requestId));
        return new ValueResponse(taskExecuteCode);
    }

    @PostMapping("/control")
    @DangerousSampleOperation
    @Operation(
            operationId = "task.control",
            summary = "控制任务 / Control task",
            description = "危险操作：会暂停、恢复或终止现场任务。"
                    + " Dangerous operation: pauses, resumes, or stops a physical task execution.",
            responses = @ApiResponse(
                    responseCode = "403",
                    description = "危险操作默认关闭 / Dangerous operation disabled by default",
                    content = @Content(mediaType = "application/problem+json", examples = {
                            @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                            @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                    })))
    public ValueResponse control(
            @Valid @RequestBody TaskInputs.Control input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String value = service.control(input.toSdkRequest(), SampleRequestOptions.from(requestId));
        return new ValueResponse(value);
    }

    @PostMapping("/batch-cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DangerousSampleOperation
    @Operation(
            operationId = "task.batchCancel",
            summary = "批量取消任务 / Batch cancel tasks",
            description = "危险操作：会批量取消指定任务执行。"
                    + " Dangerous operation: cancels the selected task executions.",
            responses = @ApiResponse(
                    responseCode = "403",
                    description = "危险操作默认关闭 / Dangerous operation disabled by default",
                    content = @Content(mediaType = "application/problem+json", examples = {
                            @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                            @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                    })))
    public void batchCancel(
            @Valid @RequestBody TaskInputs.BatchCancel input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        service.batchCancel(input.toSdkRequest(), SampleRequestOptions.from(requestId));
    }

    @PostMapping("/page")
    @Operation(operationId = "task.page", summary = "分页查询任务执行记录 / Page task executions")
    public PageView<TaskExecuteInfo> page(
            @Valid @RequestBody TaskInputs.Page input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return PageView.from(service.page(input.toSdkRequest(), SampleRequestOptions.from(requestId)));
    }
}
