package com.deeprobotics.station.openapi.sample.web;

import com.deeprobotics.station.openapi.sample.safety.DangerousSampleOperation;
import com.deeprobotics.station.openapi.sample.service.DogSampleService;
import com.deeprobotics.station.openapi.sample.web.dto.DogInputs;
import com.deeprobotics.station.openapi.sample.web.view.PageView;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogAlarmInfo;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogPosition;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 四足狗查询与控制的薄 Web 示例，对应 SDK 的 {@code client.dog()} 分组。
 *
 * <p>Controller 只处理 HTTP 输入输出和危险操作标记；平台暂无实时坐标时用 {@code 204} 明确表示空结果。</p>
 */
@Validated
@RestController
@RequestMapping("/sample/api/dog")
@Tag(name = "Dog", description = "四足狗查询与控制 / Robot dog queries and controls")
@ApiResponse(
        responseCode = "400",
        description = "参数校验失败 / Validation failed",
        content = @Content(mediaType = "application/problem+json", examples = {
                @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.VALIDATION_ZH),
                @ExampleObject(name = "en_US", value = SampleOpenApiExamples.VALIDATION_EN)
        }))
public class DogSampleController {

    private final DogSampleService service;

    public DogSampleController(DogSampleService service) {
        this.service = service;
    }

    @PostMapping("/get-state")
    @Operation(operationId = "dog.getState", summary = "查询四足狗状态 / Get robot dog state")
    public DogState getState(
            @Valid @RequestBody DogInputs.State input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return service.getState(input.toSdkRequest(), SampleRequestOptions.from(requestId));
    }

    @DangerousSampleOperation
    @PostMapping("/charge-control")
    @Operation(
            operationId = "dog.chargeControl",
            summary = "充电返航控制 / Charge return control",
            description = "危险操作：向现场四足狗下发充电返航控制。"
                    + " Dangerous operation: sends a charge-return command to a physical robot dog.",
            responses = @ApiResponse(
                    responseCode = "403",
                    description = "危险操作默认关闭 / Dangerous operation disabled by default",
                    content = @Content(mediaType = "application/problem+json", examples = {
                            @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                            @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                    })))
    public ResponseEntity<Void> chargeControl(
            @Valid @RequestBody DogInputs.ChargeControl input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        service.chargeControl(input.toSdkRequest(), SampleRequestOptions.from(requestId));
        return ResponseEntity.noContent().build();
    }

    @DangerousSampleOperation
    @PostMapping("/control")
    @Operation(
            operationId = "dog.control",
            summary = "四足狗运动控制 / Control robot dog",
            description = "危险操作：向现场四足狗下发模式或运动指令。"
                    + " Dangerous operation: sends mode or motion commands to a physical robot dog.",
            responses = @ApiResponse(
                    responseCode = "403",
                    description = "危险操作默认关闭 / Dangerous operation disabled by default",
                    content = @Content(mediaType = "application/problem+json", examples = {
                            @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                            @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                    })))
    public ResponseEntity<Void> control(
            @Valid @RequestBody DogInputs.Control input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        service.control(input.toSdkRequest(), SampleRequestOptions.from(requestId));
        return ResponseEntity.noContent().build();
    }

    @DangerousSampleOperation
    @PostMapping("/set-position")
    @Operation(
            operationId = "dog.setPosition",
            summary = "下发路网目标点 / Set robot dog position target",
            description = "危险操作：向现场四足狗下发路网选点坐标。"
                    + " Dangerous operation: sends a road-map target position to a physical robot dog.",
            responses = @ApiResponse(
                    responseCode = "403",
                    description = "危险操作默认关闭 / Dangerous operation disabled by default",
                    content = @Content(mediaType = "application/problem+json", examples = {
                            @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                            @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                    })))
    public ResponseEntity<Void> setPosition(
            @Valid @RequestBody DogInputs.SetPosition input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        service.setPosition(input.toSdkRequest(), SampleRequestOptions.from(requestId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/page-alarms")
    @Operation(operationId = "dog.pageAlarms", summary = "分页查询四足狗告警 / Page robot dog alarms")
    public PageView<DogAlarmInfo> pageAlarms(
            @Valid @RequestBody DogInputs.PageAlarms input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return PageView.from(service.pageAlarms(input.toSdkRequest(), SampleRequestOptions.from(requestId)));
    }

    @PostMapping("/get-position")
    @Operation(operationId = "dog.getPosition", summary = "查询四足狗实时坐标 / Get robot dog position")
    public ResponseEntity<DogPosition> getPosition(
            @Valid @RequestBody DogInputs.Position input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        // 平台暂无实时坐标时返回 204，比返回 null JSON 更容易被调用方明确判断。
        return service.getPosition(input.toSdkRequest(), SampleRequestOptions.from(requestId))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
