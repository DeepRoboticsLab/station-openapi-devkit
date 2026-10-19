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
 * <p>创建、启停和删除操作由安全门禁保护，查询操作则直接返回适合 Swagger 展示的结果。</p>
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

    /**
     * 切换任务模板的启用状态，默认由危险操作门禁拦截。
     *
     * <p>停用会清除未执行任务，启用不会自动下发；成功后返回 204。</p>
     *
     * @param input 任务模板编码和整数启用状态
     * @param requestId 可选链路标识，不是业务幂等键
     */
    @PostMapping("/switch-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DangerousSampleOperation
    @Operation(
            operationId = "taskTemplate.switchStatus",
            summary = "启停任务模板 / Enable or disable task template",
            description = "危险操作：平台 v1.3.3 起支持。停用会清除未执行任务；启用后必须再次下发。"
                    + " Dangerous operation: requires platform v1.3.3 or later. Disabling removes pending tasks;"
                    + " enabling requires issuing the template again.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "状态已更新 / Status updated"),
                    @ApiResponse(
                            responseCode = "403",
                            description = "危险操作默认关闭 / Dangerous operation disabled by default",
                            content = @Content(mediaType = "application/problem+json", examples = {
                                    @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                                    @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                            }))
            })
    public void switchStatus(
            @Valid @RequestBody TaskTemplateInputs.SwitchStatus input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        service.switchStatus(input.toSdkRequest(), SampleRequestOptions.from(requestId));
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
