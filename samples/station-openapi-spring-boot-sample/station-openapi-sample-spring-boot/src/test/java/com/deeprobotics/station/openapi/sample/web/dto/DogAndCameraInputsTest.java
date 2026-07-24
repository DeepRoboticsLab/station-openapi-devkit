package com.deeprobotics.station.openapi.sample.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DogAndCameraInputsTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void dogStateMapsDogCode() {
        assertThat(new DogInputs.State("dog-001").toSdkRequest().toMap())
                .containsExactly(org.assertj.core.api.Assertions.entry("dogCode", "dog-001"));
    }

    @Test
    void dogChargeControlPreservesRawCommand() {
        assertThat(new DogInputs.ChargeControl("dog-001", "99").toSdkRequest().toMap())
                .containsEntry("dogCode", "dog-001")
                .containsEntry("command", "99");
    }

    @Test
    void dogControlPreservesRawCodes() {
        assertThat(new DogInputs.Control("dog-001", 2, 99, "0.2", 1).toSdkRequest().toMap())
                .containsEntry("dogCode", "dog-001")
                .containsEntry("type", 2)
                .containsEntry("command", 99)
                .containsEntry("value", "0.2")
                .containsEntry("speed", 1);
    }

    @Test
    void dogSetPositionKeepsCoordinateStrings() {
        assertThat(new DogInputs.SetPosition("dog-001", "edge-001", "1.20", "2.30", "0.40")
                .toSdkRequest().toMap())
                .containsEntry("dogCode", "dog-001")
                .containsEntry("edgeCode", "edge-001")
                .containsEntry("x", "1.20")
                .containsEntry("y", "2.30")
                .containsEntry("z", "0.40");
    }

    @Test
    void dogAlarmPageDelegatesTimeAndPaginationFormatting() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 13, 8, 9, 10);
        LocalDateTime end = LocalDateTime.of(2026, 7, 14, 18, 19, 20);

        assertThat(new DogInputs.PageAlarms("dog-001", start, end, "alarm-001", 2, 30)
                .toSdkRequest().toMap())
                .containsEntry("dogCode", "dog-001")
                .containsEntry("startTime", "2026-07-13 08:09:10")
                .containsEntry("endTime", "2026-07-14 18:19:20")
                .containsEntry("alarmCode", "alarm-001")
                .containsEntry("pageNo", 2)
                .containsEntry("pageSize", 30)
                .doesNotContainKey("pageNum");
    }

    @Test
    void dogPositionMapsDogCode() {
        assertThat(new DogInputs.Position("dog-001").toSdkRequest().toMap())
                .containsExactly(org.assertj.core.api.Assertions.entry("dogCode", "dog-001"));
    }

    @Test
    void cameraControlPreservesForwardCompatibleRawCodes() {
        assertThat(new CameraInputs.Control("camera-001", 99, 100, 1, 2)
                .toSdkRequest().toMap())
                .containsEntry("cameraCode", "camera-001")
                .containsEntry("ctrlType", 99)
                .containsEntry("command", 100)
                .containsEntry("value", 1)
                .containsEntry("channelNum", 2);
    }

    @Test
    void cameraResetMapsCameraCode() {
        assertThat(new CameraInputs.ResetPtz("camera-001").toSdkRequest().toMap())
                .containsExactly(org.assertj.core.api.Assertions.entry("cameraCode", "camera-001"));
    }

    @Test
    void structuralRangesUseStableMessageKeys() {
        Set<String> dogMessages = validator.validate(
                        new DogInputs.Control("dog-001", 0, 0, "0", 6))
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> cameraMessages = validator.validate(
                        new CameraInputs.Control("camera-001", 0, 0, 2, null))
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(dogMessages)
                .contains("REMOTE_API_DOG_CONTROL_TYPE_MIN", "REMOTE_API_DOG_CONTROL_SPEED_MAX")
                .doesNotContain("REMOTE_API_DOG_CONTROL_COMMAND_REQUIRED");
        assertThat(cameraMessages).contains(
                "REMOTE_API_CAMERA_CTRL_TYPE_MIN",
                "REMOTE_API_CAMERA_COMMAND_MIN",
                "REMOTE_API_CAMERA_VALUE_RANGE");
    }
}
