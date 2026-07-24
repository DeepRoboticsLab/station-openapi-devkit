package com.deeprobotics.station.openapi.sample.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TaskInputsTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void taskTemplateCreateMapsEveryFieldThroughSdkBuilder() {
        TaskTemplateInputs.Create input = new TaskTemplateInputs.Create(
                "Daily patrol",
                3,
                "routine",
                List.of("P-1", "P-2"),
                2,
                "1",
                1,
                "1",
                LocalDateTime.of(2026, 7, 13, 8, 0),
                List.of("1", "12"),
                List.of("1", "7"),
                LocalTime.of(9, 10, 11),
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59),
                30,
                3,
                LocalTime.of(10, 20, 30),
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 7, 20, 0, 0),
                LocalDateTime.of(2026, 7, 21, 0, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0));

        assertThat(input.toSdkRequest().toMap())
                .containsEntry("taskName", "Daily patrol")
                .containsEntry("executeType", 3)
                .containsEntry("patrolType", "routine")
                .containsEntry("pointCodes", "P-1,P-2")
                .containsEntry("priority", 2)
                .containsEntry("endAction", "1")
                .containsEntry("deviceLevel", 1)
                .containsEntry("isEnable", "1")
                .containsEntry("fixedStartTime", "2026-07-13 08:00:00")
                .containsEntry("cycleMonth", "1,12")
                .containsEntry("cycleWeek", "1,7")
                .containsEntry("cycleExecuteTime", "09:10:11")
                .containsEntry("cycleStartTime", "2026-07-01 00:00:00")
                .containsEntry("cycleEndTime", "2026-12-31 23:59:59")
                .containsEntry("intervalNumber", 30)
                .containsEntry("intervalType", 3)
                .containsEntry("intervalExecuteTime", "10:20:30")
                .containsEntry("intervalStartTime", "2026-07-01 00:00:00")
                .containsEntry("intervalEndTime", "2026-08-01 00:00:00")
                .containsEntry("invalidStartTime", "2026-07-20 00:00:00")
                .containsEntry("invalidEndTime", "2026-07-21 00:00:00")
                .containsEntry("invalidDailyStartTime", "12:00:00")
                .containsEntry("invalidDailyEndTime", "13:00:00");
    }

    @Test
    void taskTemplateDeleteKeepsCodesAsJsonArray() {
        assertThat(new TaskTemplateInputs.Delete(List.of("T-1", "T-2")).toSdkRequest().toMap())
                .containsEntry("taskCodes", List.of("T-1", "T-2"));
    }

    @Test
    void taskTemplateGetAndPageMapDirectly() {
        assertThat(new TaskTemplateInputs.GetByCode("T-1").toSdkRequest().toMap())
                .containsEntry("taskCode", "T-1");
        assertThat(new TaskTemplateInputs.Page("Daily", "routine", "dog-001", "2", 1, 2, 30)
                .toSdkRequest().toMap())
                .containsEntry("taskName", "Daily")
                .containsEntry("patrolType", "routine")
                .containsEntry("dogCode", "dog-001")
                .containsEntry("priority", "2")
                .containsEntry("isEnable", 1)
                .containsEntry("pageNo", 2)
                .containsEntry("pageSize", 30);
    }

    @Test
    void issueAndControlPreserveRequiredRawValues() {
        assertThat(new TaskInputs.Issue("T-1").toSdkRequest().toMap())
                .containsEntry("taskCode", "T-1");
        assertThat(new TaskInputs.Control("E-1", "99").toSdkRequest().toMap())
                .containsEntry("taskExecuteCode", "E-1")
                .containsEntry("command", "99");
    }

    @Test
    void batchCancelKeepsExecutionCodesAsJsonArray() {
        assertThat(new TaskInputs.BatchCancel(List.of("E-1", "E-2")).toSdkRequest().toMap())
                .containsEntry("taskExecuteCodeList", List.of("E-1", "E-2"));
    }

    @Test
    void taskPageMapsStatusArrayToCommaTextAndUsesPageNo() {
        assertThat(new TaskInputs.Page(
                "dog-001",
                "Daily",
                List.of("1", "2"),
                1,
                2,
                "running",
                true,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                1,
                10).toSdkRequest().toMap())
                .containsEntry("dogCode", "dog-001")
                .containsEntry("taskName", "Daily")
                .containsEntry("statusArr", "1,2")
                .containsEntry("sendStatus", 1)
                .containsEntry("priority", 2)
                .containsEntry("status", "running")
                .containsEntry("showUnExecuted", true)
                .containsEntry("patrolStartTime", "2026-07-01")
                .containsEntry("patrolEndTime", "2026-07-31")
                .containsEntry("pageNo", 1)
                .containsEntry("pageSize", 10)
                .doesNotContainKey("pageNum");
    }

    @Test
    void requiredCollectionsAndCodesUseStableMessageKeys() {
        Set<String> createMessages = validator.validate(new TaskTemplateInputs.Create(
                        "", null, null, List.of(), null, "", null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null))
                .stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        Set<String> deleteMessages = validator.validate(new TaskTemplateInputs.Delete(List.of()))
                .stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
        Set<String> batchMessages = validator.validate(new TaskInputs.BatchCancel(List.of()))
                .stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());

        assertThat(createMessages).contains(
                "REMOTE_API_TASK_NAME_REQUIRED",
                "REMOTE_API_TASK_POINT_CODES_REQUIRED",
                "REMOTE_API_TASK_END_ACTION_REQUIRED");
        assertThat(deleteMessages).contains("REMOTE_API_TASK_CODES_REQUIRED");
        assertThat(batchMessages).contains("REMOTE_API_TASK_EXECUTE_CODE_LIST_REQUIRED");
    }
}
