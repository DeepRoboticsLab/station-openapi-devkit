package com.deeprobotics.station.openapi.sample.web;

import com.deeprobotics.station.openapi.sample.service.SystemSampleService;
import com.deeprobotics.station.openapi.sample.web.view.ValueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统辅助接口的薄 Web 示例，对应 SDK 的 {@code client.system()} 分组。
 *
 * <p>服务器时间接口没有业务请求字段，适合用于确认鉴权配置和平台连通性。</p>
 */
@RestController
@RequestMapping("/sample/api/system")
@Tag(name = "System", description = "系统辅助接口 / System utilities")
@ApiResponse(
        responseCode = "400",
        description = "参数校验失败 / Validation failed",
        content = @Content(mediaType = "application/problem+json", examples = {
                @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.VALIDATION_ZH),
                @ExampleObject(name = "en_US", value = SampleOpenApiExamples.VALIDATION_EN)
        }))
public class SystemSampleController {

    private final SystemSampleService service;

    public SystemSampleController(SystemSampleService service) {
        this.service = service;
    }

    @PostMapping("/get-time-text")
    @Operation(operationId = "system.getTimeText", summary = "获取平台服务器时间 / Get platform server time")
    public ValueResponse getTimeText(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return new ValueResponse(service.getTimeText(SampleRequestOptions.from(requestId)));
    }
}
