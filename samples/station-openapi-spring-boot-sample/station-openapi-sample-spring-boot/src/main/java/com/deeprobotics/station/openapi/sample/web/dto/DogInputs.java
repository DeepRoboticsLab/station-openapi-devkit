package com.deeprobotics.station.openapi.sample.web.dto;

import com.deeprobotics.station.openapi.sdk.model.request.dog.DogAlarmPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogChargeControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogPositionRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogPositionSetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogStateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 四足狗查询与控制接口的轻量 Web 入参集合。
 *
 * <p>record 完成必填项和范围校验，再构建 SDK 请求；{@code type=2} 的数值校验由 SDK 执行。
 * SDK 不校验控制类型与命令码的组合是否受设备支持。</p>
 */
public final class DogInputs {

    private DogInputs() {
    }

    public record State(
            @Schema(description = "四足狗编码 / Robot dog code", example = "dog-001")
            @NotBlank(message = "REMOTE_API_DOG_PROP_DOG_CODE_REQUIRED") String dogCode) {

        public DogStateRequest toSdkRequest() {
            return DogStateRequest.builder().dogCode(dogCode).build();
        }
    }

    public record ChargeControl(
            @Schema(description = "四足狗编码 / Robot dog code", example = "dog-001")
            @NotBlank(message = "REMOTE_API_DOG_CODE_REQUIRED") String dogCode,
            @Schema(description = "当前 1=充电返航，2=取消返航；保留 SDK raw code / "
                    + "Current: 1=return to charge, 2=cancel; SDK raw codes remain supported",
                    example = "1")
            @NotBlank(message = "REMOTE_API_CHARGE_COMMAND_REQUIRED") String command) {

        public DogChargeControlRequest toSdkRequest() {
            return DogChargeControlRequest.builder()
                    .dogCode(dogCode)
                    .command(command)
                    .build();
        }
    }

    public record Control(
            @Schema(description = "四足狗编码 / Robot dog code", example = "dog-001")
            @NotBlank(message = "REMOTE_API_DOG_CODE_REQUIRED") String dogCode,
            @Schema(description = "当前 1=模式切换，2=运动控制；允许 SDK 支持的正整数 raw code / "
                    + "Current: 1=mode switch, 2=motion control; positive SDK raw codes remain supported",
                    example = "2")
            @NotNull(message = "REMOTE_API_DOG_CONTROL_TYPE_REQUIRED")
            @Min(value = 1, message = "REMOTE_API_DOG_CONTROL_TYPE_MIN") Integer type,
            @Schema(description = "当前 type=1 使用 6；type=2 使用 0/1/2/3/4/11/12/15/16，15=趴下，16=起立；保留 raw code / "
                    + "Current type=1 uses 6; type=2 uses 0/1/2/3/4/11/12/15/16, 15=lie down, 16=stand up; raw codes remain supported",
                    example = "1")
            @NotNull(message = "REMOTE_API_DOG_CONTROL_COMMAND_REQUIRED") Integer command,
            @Schema(description = "指令值；type=2 时为不大于 0.6 的数值字符串 / "
                    + "Command value; numeric string no greater than 0.6 when type=2",
                    example = "0.2")
            @NotBlank(message = "REMOTE_API_DOG_CONTROL_VALUE_REQUIRED") String value,
            @Schema(description = "控制指令重复下发次数，范围 1..5 / Repeat count, range 1..5", example = "1")
            @Min(value = 1, message = "REMOTE_API_DOG_CONTROL_SPEED_MIN")
            @Max(value = 5, message = "REMOTE_API_DOG_CONTROL_SPEED_MAX") Integer speed) {

        /**
         * 构建 SDK 请求；运动控制值的数值和上限由 SDK 校验。
         */
        public DogControlRequest toSdkRequest() {
            return DogControlRequest.builder()
                    .dogCode(dogCode)
                    .type(type)
                    .command(command)
                    .value(value)
                    .speed(speed)
                    .build();
        }
    }

    public record SetPosition(
            @NotBlank(message = "REMOTE_API_DOG_POS_SET_DOG_CODE_REQUIRED") String dogCode,
            @NotBlank(message = "REMOTE_API_DOG_POS_EDGE_CODE_REQUIRED") String edgeCode,
            @NotBlank(message = "REMOTE_API_DOG_POS_X_REQUIRED") String x,
            @NotBlank(message = "REMOTE_API_DOG_POS_Y_REQUIRED") String y,
            String z) {

        public DogPositionSetRequest toSdkRequest() {
            return DogPositionSetRequest.builder()
                    .dogCode(dogCode)
                    .edgeCode(edgeCode)
                    .x(x)
                    .y(y)
                    .z(z)
                    .build();
        }
    }

    public record PageAlarms(
            String dogCode,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String alarmCode,
            Integer pageNo,
            Integer pageSize) {

        public DogAlarmPageRequest toSdkRequest() {
            return DogAlarmPageRequest.builder()
                    .dogCode(dogCode)
                    .startTime(startTime)
                    .endTime(endTime)
                    .alarmCode(alarmCode)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .build();
        }
    }

    public record Position(
            @Schema(description = "四足狗编码 / Robot dog code", example = "dog-001")
            @NotBlank(message = "REMOTE_API_DOG_POS_QUERY_DOG_CODE_REQUIRED") String dogCode) {

        public DogPositionRequest toSdkRequest() {
            return DogPositionRequest.builder().dogCode(dogCode).build();
        }
    }
}
