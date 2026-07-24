package com.deeprobotics.station.openapi.sample.web.dto;

import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraResetRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 摄像机接口的轻量 Web 入参集合。
 *
 * <p>各 record 负责 HTTP 结构校验，再显式构建 SDK 请求；已知控制编码只用于说明，
 * 不会收紧 SDK 为平台新增正整数 raw code 预留的扩展入口。</p>
 */
public final class CameraInputs {

    private CameraInputs() {
    }

    public record Control(
            @Schema(description = "摄像机编码 / Camera code", example = "camera-001")
            @NotBlank(message = "REMOTE_API_CAMERA_CODE_REQUIRED") String cameraCode,
            @Schema(description = "当前 3=云台，4=可见光镜头，5=红外镜头；保留正整数 raw code / "
                    + "Current: 3=PTZ, 4=visible lens, 5=infrared lens; positive raw codes remain supported",
                    example = "3")
            @NotNull(message = "REMOTE_API_CAMERA_CTRL_TYPE_REQUIRED")
            @Min(value = 1, message = "REMOTE_API_CAMERA_CTRL_TYPE_MIN") Integer ctrlType,
            @Schema(description = "当前云台指令 1..8，镜头指令 1..5；保留正整数 raw code / "
                    + "Current PTZ commands: 1..8, lens commands: 1..5; positive raw codes remain supported",
                    example = "1")
            @NotNull(message = "REMOTE_API_CAMERA_COMMAND_REQUIRED")
            @Min(value = 1, message = "REMOTE_API_CAMERA_COMMAND_MIN") Integer command,
            @Schema(description = "0=停止，1=启动 / 0=stop, 1=start", example = "1")
            @Min(value = 0, message = "REMOTE_API_CAMERA_VALUE_RANGE")
            @Max(value = 1, message = "REMOTE_API_CAMERA_VALUE_RANGE") Integer value,
            Integer channelNum) {

        /**
         * 构建 SDK 请求，并原样保留正整数控制编码，避免 Sample 先于平台限制扩展值。
         */
        public CameraControlRequest toSdkRequest() {
            return CameraControlRequest.builder()
                    .cameraCode(cameraCode)
                    .ctrlType(ctrlType)
                    .command(command)
                    .value(value)
                    .channelNum(channelNum)
                    .build();
        }
    }

    public record ResetPtz(
            @Schema(description = "摄像机编码 / Camera code", example = "camera-001")
            @NotBlank(message = "REMOTE_API_CAMERA_CODE_REQUIRED") String cameraCode) {

        public CameraResetRequest toSdkRequest() {
            return CameraResetRequest.builder().cameraCode(cameraCode).build();
        }
    }
}
