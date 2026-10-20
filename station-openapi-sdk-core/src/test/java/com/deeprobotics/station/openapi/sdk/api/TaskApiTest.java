package com.deeprobotics.station.openapi.sdk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.auth.AccessTokenAuthProvider;
import com.deeprobotics.station.openapi.sdk.auth.Credential;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTimeoutException;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTimeoutException;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.internal.DefaultTaskApi;
import com.deeprobotics.station.openapi.sdk.internal.OpenApiExecutor;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskBatchCancelRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskControlCommand;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskIssueRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.task.TaskExecuteInfo;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 任务执行 API 契约测试。
 *
 * <p>任务下发、暂停、恢复、终止和批量取消都有业务副作用，必须标记为 TASK_CONTROL，
 * 不自动重试；任务执行分页只是查询，按 READ_ONLY 处理。</p>
 */
class TaskApiTest {

    private static final JsonCodec JSON = JsonCodec.defaultCodec();

    @Test
    void taskEndpointsUseExpectedPathsAndOperationIntents() {
        Map<String, OperationIntent> endpointIntents = readEndpointDefinitions(DefaultTaskApi.class).stream()
                .collect(Collectors.toMap(EndpointDefinition::path, EndpointDefinition::intent));

        assertThat(endpointIntents)
                .containsEntry("/remoteApi/issue", OperationIntent.TASK_CONTROL)
                .containsEntry("/remoteApi/taskCtrl", OperationIntent.TASK_CONTROL)
                .containsEntry("/remoteApi/batchCancelTask", OperationIntent.TASK_CONTROL)
                .containsEntry("/remoteApi/taskPage", OperationIntent.READ_ONLY)
                .hasSize(4);
    }

    @Test
    void taskApisUsePostJsonParseResponsesAndPreserveWireShapes() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess("TE-001")
                .enqueueSuccess("paused")
                .enqueueSuccess("resumed")
                .enqueueSuccess("stopped")
                .enqueueSuccess(null)
                .enqueueSuccess(page(List.of(taskExecuteInfo()), 1L, 1L, 10L, 1L));
        TaskApi api = new DefaultTaskApi(newExecutor(transport));

        String taskExecuteCode = api.issue(TaskIssueRequest.builder().taskCode(" TM-001 ").build());
        String pauseResult = api.control(TaskControlRequest.builder()
                .taskExecuteCode("TE-001")
                .command(TaskControlCommand.PAUSE)
                .build());
        String resumeResult = api.control(TaskControlRequest.builder()
                .taskExecuteCode("TE-001")
                .command(TaskControlCommand.RESUME)
                .build());
        String stopResult = api.control(TaskControlRequest.builder()
                .taskExecuteCode("TE-001")
                .command(TaskControlCommand.STOP)
                .build());
        api.batchCancel(TaskBatchCancelRequest.builder()
                .taskExecuteCodeList(List.of("TE-001", "TE-002"))
                .build());
        PageResult<TaskExecuteInfo> page = api.page(TaskPageRequest.builder()
                .dogCode("DOG-001")
                .taskName("例行巡视")
                .statusArr(List.of("2", "3"))
                .sendStatus(1)
                .priority(4)
                .status("2")
                .patrolStartTime(LocalDate.of(2026, 7, 1))
                .patrolEndTime(LocalDate.of(2026, 7, 31))
                .pageNo(1)
                .pageSize(0)
                .build());

        assertThat(taskExecuteCode).isEqualTo("TE-001");
        assertThat(pauseResult).isEqualTo("paused");
        assertThat(resumeResult).isEqualTo("resumed");
        assertThat(stopResult).isEqualTo("stopped");
        assertThat(page.records()).hasSize(1);
        assertThat(page.records().get(0).taskExecuteCode()).isEqualTo("TE-001");
        assertThat(page.records().get(0).planStartTime())
                .isEqualTo(LocalDateTime.of(2026, 7, 9, 9, 0, 0));
        assertThat(page.records().get(0).taskRoad()).containsEntry("A", "B");
        assertThat(transport.downloadRequests()).isEmpty();
        assertThat(transport.jsonRequests().stream()
                .map(request -> request.url().getPath())
                .collect(Collectors.toList()))
                .containsExactly(
                        "/remoteApi/issue",
                        "/remoteApi/taskCtrl",
                        "/remoteApi/taskCtrl",
                        "/remoteApi/taskCtrl",
                        "/remoteApi/batchCancelTask",
                        "/remoteApi/taskPage");

        assertThat(parseBody(transport.jsonRequests().get(0))).containsEntry("taskCode", "TM-001");
        assertThat(parseBody(transport.jsonRequests().get(1)))
                .containsEntry("taskExecuteCode", "TE-001")
                .containsEntry("command", "2");
        assertThat(parseBody(transport.jsonRequests().get(2))).containsEntry("command", "3");
        assertThat(parseBody(transport.jsonRequests().get(3))).containsEntry("command", "4");
        assertThat(parseBody(transport.jsonRequests().get(4)))
                .containsEntry("taskExecuteCodeList", List.of("TE-001", "TE-002"));
        assertThat(parseBody(transport.jsonRequests().get(5)))
                .containsEntry("dogCode", "DOG-001")
                .containsEntry("taskName", "例行巡视")
                .containsEntry("statusArr", "2,3")
                .containsEntry("sendStatus", 1)
                .containsEntry("priority", 4)
                .containsEntry("status", "2")
                .containsEntry("patrolStartTime", "2026-07-01")
                .containsEntry("patrolEndTime", "2026-07-31")
                .containsEntry("pageNo", 1)
                .containsEntry("pageSize", 10)
                .doesNotContainKey("showUnExecuted")
                .doesNotContainKey("pageNum");
    }

    @Test
    void taskPageSendsShowUnExecutedOnlyWhenCustomerSetsIt() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess(page(List.of(), 0L, 1L, 10L, 0L));
        TaskApi api = new DefaultTaskApi(newExecutor(transport));

        api.page(TaskPageRequest.builder()
                .showUnExecuted(true)
                .pageNo(1)
                .pageSize(10)
                .build());

        assertThat(parseBody(transport.jsonRequests().get(0))).containsEntry("showUnExecuted", true);
    }

    @Test
    void taskRoadIsAnImmutableNullSafeSnapshot() {
        LinkedHashMap<String, String> source = new LinkedHashMap<>();
        source.put("start", "NODE-001");
        TaskExecuteInfo info = taskExecuteInfo(source);

        source.put("end", "NODE-002");

        assertThat(info.taskRoad()).containsExactly(Map.entry("start", "NODE-001"));
        assertThatThrownBy(() -> info.taskRoad().put("other", "NODE-003"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(taskExecuteInfo(null).taskRoad()).isEmpty();
    }

    @Test
    void taskRequestsValidateRequiredFieldsLocally() {
        RecordingTransport transport = new RecordingTransport();
        TaskApi api = new DefaultTaskApi(newExecutor(transport));

        assertThatThrownBy(() -> api.issue(TaskIssueRequest.builder().taskCode(" ").build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("taskCode");
        assertThatThrownBy(() -> api.control(TaskControlRequest.builder()
                .command(TaskControlCommand.PAUSE)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("taskExecuteCode");
        assertThatThrownBy(() -> api.control(TaskControlRequest.builder()
                .taskExecuteCode("TE-001")
                .command((String) null)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("command");
        assertThatThrownBy(() -> api.batchCancel(TaskBatchCancelRequest.builder()
                .taskExecuteCodeList(List.of(" "))
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("taskExecuteCodeList");
        assertThatThrownBy(() -> api.page(TaskPageRequest.builder().pageNo(0).build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("pageNo");

        assertThat(transport.jsonRequests()).isEmpty();
        assertThat(transport.downloadRequests()).isEmpty();
    }

    @Test
    void taskControlMethodsDoNotRetryAndTimeoutCarriesUnknownResult() {
        RecordingTransport transport = new RecordingTransport().enqueueTimeout();
        TaskApi api = new DefaultTaskApi(newExecutor(transport));

        assertThatThrownBy(() -> api.control(TaskControlRequest.builder()
                .taskExecuteCode("TE-001")
                .command(TaskControlCommand.STOP)
                .build()))
                .isInstanceOfSatisfying(StationOpenApiTimeoutException.class, ex -> {
                    assertThat(ex.resultUnknown()).isTrue();
                    assertThat(ex.path()).isEqualTo("/remoteApi/taskCtrl");
                    assertThat(ex.operation()).isEqualTo("task.control");
                });

        assertThat(transport.jsonRequests()).hasSize(1);
    }

    private static OpenApiExecutor newExecutor(RecordingTransport transport) {
        StationOpenApiConfig config = StationOpenApiConfig.builder()
                .endpoint("http://host:23288")
                .accessToken("token-value")
                .build();
        return new OpenApiExecutor(config, new AccessTokenAuthProvider(Credential.accessToken("token-value")),
                transport, JSON);
    }

    private static Map<String, Object> parseBody(HttpRequest request) {
        return JSON.read(request.body(), new TypeRef<LinkedHashMap<String, Object>>() {
        });
    }

    private static Map<String, Object> page(List<Map<String, Object>> records, long total, long current, long size,
            long pages) {
        LinkedHashMap<String, Object> page = new LinkedHashMap<>();
        page.put("records", records);
        page.put("total", total);
        page.put("current", current);
        page.put("size", size);
        page.put("pages", pages);
        return page;
    }

    private static LinkedHashMap<String, Object> taskExecuteInfo() {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("stationName", "杭州站");
        body.put("taskCode", "TM-001");
        body.put("taskName", "例行巡视");
        body.put("taskExecuteCode", "TE-001");
        body.put("priority", 4);
        body.put("dogCode", "DOG-001");
        body.put("taskType", 1);
        body.put("executeType", 1);
        body.put("patrolType", "200");
        body.put("pointCodes", "P001,P002");
        body.put("pointCount", 2);
        body.put("taskProgress", "1/2");
        body.put("planStartTime", "2026-07-09 09:00:00");
        body.put("startTime", "2026-07-09 09:01:00");
        body.put("endTime", "2026-07-09 09:10:00");
        body.put("allCount", 2);
        body.put("normalCount", 1);
        body.put("alarmCount", 1);
        body.put("recognitionNormalCount", 1);
        body.put("recognitionErrorCount", 0);
        body.put("missCount", 0);
        body.put("taskRoad", linkedMapOf("A", "B"));
        body.put("status", "2");
        body.put("sendStatus", 1);
        body.put("endAction", 1);
        body.put("unknownField", "ignored");
        return body;
    }

    private static TaskExecuteInfo taskExecuteInfo(Map<String, String> taskRoad) {
        return new TaskExecuteInfo(
                null, // stationName
                null, // taskCode
                null, // taskName
                null, // taskExecuteCode
                null, // priority
                null, // dogCode
                null, // taskType
                null, // executeType
                null, // patrolType
                null, // pointCodes
                null, // pointCount
                null, // taskProgress
                null, // planStartTime
                null, // startTime
                null, // endTime
                null, // allCount
                null, // normalCount
                null, // alarmCount
                null, // recognitionNormalCount
                null, // recognitionErrorCount
                null, // missCount
                taskRoad,
                null, // status
                null, // sendStatus
                null); // endAction
    }

    private static LinkedHashMap<String, Object> linkedMapOf(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues length must be even");
        }
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private static Set<EndpointDefinition> readEndpointDefinitions(Class<?> type) {
        return List.of(type.getDeclaredFields()).stream()
                .filter(field -> field.getType() == EndpointDefinition.class)
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        return (EndpointDefinition) field.get(null);
                    } catch (IllegalAccessException ex) {
                        throw new AssertionError(ex);
                    }
                })
                .collect(Collectors.toSet());
    }

    private static final class RecordingTransport implements HttpTransport {
        private final Queue<Object> outcomes = new ArrayDeque<>();
        private final List<HttpRequest> jsonRequests = new ArrayList<>();
        private final List<HttpRequest> downloadRequests = new ArrayList<>();

        private RecordingTransport enqueueSuccess(Object data) {
            outcomes.add(new HttpResponse(200, Map.of(), JSON.writeBytes(successBody(data))));
            return this;
        }

        private RecordingTransport enqueueTimeout() {
            outcomes.add(new HttpTimeoutException("task control timeout"));
            return this;
        }

        @Override
        public HttpResponse postJson(HttpRequest request) {
            jsonRequests.add(request);
            Object outcome = outcomes.remove();
            if (outcome instanceof HttpTimeoutException timeoutException) {
                throw timeoutException;
            }
            return (HttpResponse) outcome;
        }

        @Override
        public HttpResponse download(HttpRequest request, OutputStream outputStream) {
            downloadRequests.add(request);
            throw new AssertionError("task APIs must use postJson, not download");
        }

        @Override
        public void close() {
        }

        private List<HttpRequest> jsonRequests() {
            return jsonRequests;
        }

        private List<HttpRequest> downloadRequests() {
            return downloadRequests;
        }

        private static LinkedHashMap<String, Object> successBody(Object data) {
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("code", 200);
            body.put("msg", "OK");
            body.put("data", data);
            return body;
        }
    }
}
