package com.deeprobotics.station.openapi.sample.web;

import com.deeprobotics.station.openapi.sample.safety.DangerousSampleOperation;
import com.deeprobotics.station.openapi.sample.service.TaskTemplateSampleService;
import com.deeprobotics.station.openapi.sample.web.dto.TaskTemplateInputs;
import com.deeprobotics.station.openapi.sample.web.view.PageView;
import com.deeprobotics.station.openapi.sample.web.view.ValueResponse;
import com.deeprobotics.station.openapi.sdk.model.response.tasktemplate.TaskTemplateInfo;
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
 * 任务模板管理的薄 Web 示例，对应 SDK 的 {@code client.taskTemplate()} 分组。
 *
 * <p>创建和删除操作由安全门禁保护，查询操作则直接返回适合 Swagger 展示的结果。</p>
 */
@RestController
@RequestMapping("/sample/api/task-template")
@Tag(name = "Task Template", description = "任务模板 / Task templates")
@ApiResponse(
        responseCode = "400",
        description = "参数校验失败 / Validation failed",
        content = @Content(mediaType = "application/problem+json", examples = {
                @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.VALIDATION_ZH),
                @ExampleObject(name = "en_US", value = SampleOpenApiExamples.VALIDATION_EN)
        }))
public class TaskTemplateSampleController {

    private final TaskTemplateSampleService service;

    public TaskTemplateSampleController(TaskTemplateSampleService service) {
        this.service = service;
    }

    @PostMapping("/create")
    @DangerousSampleOperation
    @Operation(
            operationId = "taskTemplate.create",
            summary = "创建任务模板 / Create task template",
            description = "危险操作：会在平台新增任务模板。"
                    + " Dangerous operation: creates a task template on the platform.",
            responses = @ApiResponse(
                    responseCode = "403",
                    description = "危险操作默认关闭 / Dangerous operation disabled by default",
                    content = @Content(mediaType = "application/problem+json", examples = {
                            @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                            @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                    })))
    public ValueResponse create(
            @Valid @RequestBody TaskTemplateInputs.Create input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String taskCode = service.create(input.toSdkRequest(), SampleRequestOptions.from(requestId));
        return new ValueResponse(taskCode);
    }

    @PostMapping("/delete-by-codes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DangerousSampleOperation
    @Operation(
            operationId = "taskTemplate.deleteByCodes",
            summary = "批量删除任务模板 / Delete task templates",
            description = "危险操作：会从平台删除指定任务模板。"
                    + " Dangerous operation: deletes the selected task templates from the platform.",
            responses = @ApiResponse(
                    responseCode = "403",
                    description = "危险操作默认关闭 / Dangerous operation disabled by default",
                    content = @Content(mediaType = "application/problem+json", examples = {
                            @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                            @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                    })))
    public void deleteByCodes(
            @Valid @RequestBody TaskTemplateInputs.Delete input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        service.deleteByCodes(input.toSdkRequest(), SampleRequestOptions.from(requestId));
    }

    @PostMapping("/get-by-code")
    @Operation(operationId = "taskTemplate.getByCode", summary = "查询任务模板详情 / Get task template")
    public TaskTemplateInfo getByCode(
            @Valid @RequestBody TaskTemplateInputs.GetByCode input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return service.getByCode(input.toSdkRequest(), SampleRequestOptions.from(requestId));
    }

    @PostMapping("/page")
    @Operation(operationId = "taskTemplate.page", summary = "分页查询任务模板 / Page task templates")
    public PageView<TaskTemplateInfo> page(
            @Valid @RequestBody TaskTemplateInputs.Page input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return PageView.from(service.page(input.toSdkRequest(), SampleRequestOptions.from(requestId)));
    }
}
