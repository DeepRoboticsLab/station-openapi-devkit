package com.deeprobotics.station.openapi.sample.web;

import com.deeprobotics.station.openapi.sample.safety.DangerousSampleOperation;
import com.deeprobotics.station.openapi.sample.service.CameraSampleService;
import com.deeprobotics.station.openapi.sample.web.dto.CameraInputs;
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
 * 摄像机与云台控制的薄 Web 示例，对应 SDK 的 {@code client.camera()} 分组。
 *
 * <p>两个接口都会影响现场设备，因此只负责接收已校验输入，并通过危险操作门禁后交给 Service。</p>
 */
@Validated
@RestController
@RequestMapping("/sample/api/camera")
@Tag(name = "Camera", description = "摄像机与云台控制 / Camera and PTZ controls")
@ApiResponse(
        responseCode = "400",
        description = "参数校验失败 / Validation failed",
        content = @Content(mediaType = "application/problem+json", examples = {
                @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.VALIDATION_ZH),
                @ExampleObject(name = "en_US", value = SampleOpenApiExamples.VALIDATION_EN)
        }))
public class CameraSampleController {

    private final CameraSampleService service;

    public CameraSampleController(CameraSampleService service) {
        this.service = service;
    }

    @DangerousSampleOperation
    @PostMapping("/control")
    @Operation(
            operationId = "camera.control",
            summary = "控制摄像机 / Control camera",
            description = "危险操作：向现场摄像机下发云台或镜头控制指令。"
                    + " Dangerous operation: sends PTZ or lens commands to a physical camera.",
            responses = @ApiResponse(
                    responseCode = "403",
                    description = "危险操作默认关闭 / Dangerous operation disabled by default",
                    content = @Content(mediaType = "application/problem+json", examples = {
                            @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                            @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                    })))
    public ResponseEntity<Void> control(
            @Valid @RequestBody CameraInputs.Control input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        service.control(input.toSdkRequest(), SampleRequestOptions.from(requestId));
        return ResponseEntity.noContent().build();
    }

    @DangerousSampleOperation
    @PostMapping("/reset-ptz")
    @Operation(
            operationId = "camera.resetPtz",
            summary = "复位摄像机云台 / Reset camera PTZ",
            description = "危险操作：复位现场摄像机云台。"
                    + " Dangerous operation: resets a physical camera PTZ.",
            responses = @ApiResponse(
                    responseCode = "403",
                    description = "危险操作默认关闭 / Dangerous operation disabled by default",
                    content = @Content(mediaType = "application/problem+json", examples = {
                            @ExampleObject(name = "zh_CN", value = SampleOpenApiExamples.DANGEROUS_ZH),
                            @ExampleObject(name = "en_US", value = SampleOpenApiExamples.DANGEROUS_EN)
                    })))
    public ResponseEntity<Void> resetPtz(
            @Valid @RequestBody CameraInputs.ResetPtz input,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        service.resetPtz(input.toSdkRequest(), SampleRequestOptions.from(requestId));
        return ResponseEntity.noContent().build();
    }
}
