package com.deeprobotics.station.openapi.sdk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
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
import com.deeprobotics.station.openapi.sdk.internal.DefaultTaskTemplateApi;
import com.deeprobotics.station.openapi.sdk.internal.OpenApiExecutor;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateCreateRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateDeleteRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateGetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplatePageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateStatusRequest;
import com.deeprobotics.station.openapi.sdk.model.response.tasktemplate.TaskTemplateInfo;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
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
 * 任务模板 API 契约测试。
 *
 * <p>任务模板新增和删除属于业务写入，连接失败时操作可能已经生效或部分完成，因此必须按
 * BUSINESS_WRITE 处理，不允许 SDK 默认重试。查询和分页保持 READ_ONLY。</p>
 */
class TaskTemplateApiTest {

    private static final JsonCodec JSON = JsonCodec.defaultCodec();

    @Test
    void taskTemplateEndpointsUseExpectedPathsAndOperationIntents() {
        Map<String, OperationIntent> endpointIntents = readEndpointDefinitions(DefaultTaskTemplateApi.class).stream()
                .collect(Collectors.toMap(EndpointDefinition::path, EndpointDefinition::intent));

        assertThat(endpointIntents)
                .containsEntry("/remoteApi/onlySaveTaskModel", OperationIntent.BUSINESS_WRITE)
                .containsEntry("/remoteApi/switchTaskModelStatus", OperationIntent.BUSINESS_WRITE)
                .containsEntry("/remoteApi/removeTaskModelByCodes", OperationIntent.BUSINESS_WRITE)
                .containsEntry("/remoteApi/taskModelByCode", OperationIntent.READ_ONLY)
                .containsEntry("/remoteApi/modelPage", OperationIntent.READ_ONLY)
                .hasSize(5);
    }

    @Test
    void switchStatusSendsIntegerStateAndAcceptsNullDataWithRequestOptions() {
        RecordingTransport transport = new RecordingTransport().enqueueSuccess(null).enqueueSuccess(null);
        TaskTemplateApi api = new DefaultTaskTemplateApi(newExecutor(transport));

        api.switchStatus(TaskTemplateStatusRequest.builder().taskCode(" TM-001 ").isEnable(0).build(),
                RequestOptions.builder().requestId("template-status-001")
                        .readTimeout(Duration.ofSeconds(2)).callTimeout(Duration.ofSeconds(3)).build());
        api.switchStatus(TaskTemplateStatusRequest.builder().taskCode("TM-001").isEnable(1).build());

        assertThat(transport.jsonRequests()).hasSize(2);
        HttpRequest request = transport.jsonRequests().get(0);
        assertThat(request.url().getPath()).isEqualTo("/remoteApi/switchTaskModelStatus");
        assertThat(request.headers()).containsEntry("X-Request-Id", "template-status-001");
        assertThat(request.readTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(request.callTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(parseBody(request)).containsExactly(Map.entry("taskCode", "TM-001"), Map.entry("isEnable", 0));
        assertThat(parseBody(transport.jsonRequests().get(1))).containsEntry("isEnable", 1);
    }

    @Test
    void switchStatusRejectsMissingAndInvalidFieldsBeforeIo() {
        RecordingTransport transport = new RecordingTransport();
        TaskTemplateApi api = new DefaultTaskTemplateApi(newExecutor(transport));

        for (String taskCode : new String[] {null, "", " "}) {
            assertThatThrownBy(() -> api.switchStatus(
                    TaskTemplateStatusRequest.builder().taskCode(taskCode).isEnable(1).build()))
                    .isInstanceOf(StationOpenApiClientException.class).hasMessageContaining("taskCode");
        }
        for (Integer state : new Integer[] {null, -1, 2}) {
            assertThatThrownBy(() -> api.switchStatus(
                    TaskTemplateStatusRequest.builder().taskCode("TM-001").isEnable(state).build()))
                    .isInstanceOf(StationOpenApiClientException.class).hasMessageContaining("isEnable");
        }
        assertThat(transport.jsonRequests()).isEmpty();
    }

    @Test
    void switchStatusTimeoutIsUnknownAndNeverRetriedEvenWithDefaultRetryOverride() {
        RecordingTransport transport = new RecordingTransport().enqueueTimeout().enqueueSuccess(null);
        TaskTemplateApi api = new DefaultTaskTemplateApi(newExecutor(transport));

        assertThatThrownBy(() -> api.switchStatus(
                TaskTemplateStatusRequest.builder().taskCode("TM-001").isEnable(0).build(),
                RequestOptions.builder().retryPolicy(RequestOptions.RetryPolicy.DEFAULT).build()))
                .isInstanceOfSatisfying(StationOpenApiTimeoutException.class, ex -> {
                    assertThat(ex.resultUnknown()).isTrue();
                    assertThat(ex.path()).isEqualTo("/remoteApi/switchTaskModelStatus");
                    assertThat(ex.operation()).isEqualTo("taskTemplate.switchStatus");
                });
        assertThat(transport.jsonRequests()).hasSize(1);
    }

    @Test
    void taskTemplateApisUsePostJsonParseResponsesAndPreserveWireShapes() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess("TM-001")
                .enqueueSuccess(null)
                .enqueueSuccess(taskTemplateInfo("TM-001", "例行巡视"))
                .enqueueSuccess(page(List.of(taskTemplateInfo("TM-002", "周期巡视")), 1L, 1L, 20L, 1L));
        TaskTemplateApi api = new DefaultTaskTemplateApi(newExecutor(transport));

        String taskCode = api.create(TaskTemplateCreateRequest.builder()
                .taskName(" 例行巡视 ")
                .executeType(3)
                .patrolType("200")
                .pointCodes(List.of("P001", "P002"))
                .priority(4)
                .deviceLevel(3)
                .isEnable("1")
                .fixedStartTime(LocalDateTime.of(2026, 7, 9, 9, 30, 0))
                .cycleMonth(List.of("1", "2", "12"))
                .cycleWeek(List.of("1", "3", "5"))
                .cycleExecuteTime(LocalTime.of(10, 20, 30))
                .cycleStartTime(LocalDateTime.of(2026, 7, 10, 0, 0, 0))
                .cycleEndTime(LocalDateTime.of(2026, 8, 10, 23, 59, 59))
                .intervalNumber(2)
                .intervalType(1)
                .intervalExecuteTime(LocalTime.of(8, 0, 0))
                .intervalStartTime(LocalDateTime.of(2026, 7, 11, 0, 0, 0))
                .intervalEndTime(LocalDateTime.of(2026, 8, 11, 23, 59, 59))
                .invalidStartTime(LocalDateTime.of(2026, 7, 12, 0, 0, 0))
                .invalidEndTime(LocalDateTime.of(2026, 7, 13, 0, 0, 0))
                .invalidDailyStartTime(LocalTime.of(12, 0, 0))
                .invalidDailyEndTime(LocalTime.of(13, 30, 0))
                .build());
        api.deleteByCodes(TaskTemplateDeleteRequest.builder().taskCodes(List.of("TM-001", "TM-002")).build());
        TaskTemplateInfo detail = api.getByCode(TaskTemplateGetRequest.builder().taskCode("TM-001").build());
        PageResult<TaskTemplateInfo> page = api.page(TaskTemplatePageRequest.builder()
                .taskName("巡视")
                .patrolType("200")
                .dogCode("DOG-001")
                .priority("4")
                .isEnable(1)
                .pageNo(1)
                .pageSize(200)
                .build());

        assertThat(taskCode).isEqualTo("TM-001");
        assertThat(detail.taskCode()).isEqualTo("TM-001");
        assertThat(detail.taskName()).isEqualTo("例行巡视");
        assertThat(detail.createTime()).isEqualTo(LocalDateTime.of(2026, 7, 9, 10, 20, 30));
        assertThat(page.records()).hasSize(1);
        assertThat(page.records().get(0).planTimeContent()).isEqualTo("每天 10:20:30");
        assertThat(transport.downloadRequests()).isEmpty();
        assertThat(transport.jsonRequests().stream()
                .map(request -> request.url().getPath())
                .collect(Collectors.toList()))
                .containsExactly(
                        "/remoteApi/onlySaveTaskModel",
                        "/remoteApi/removeTaskModelByCodes",
                        "/remoteApi/taskModelByCode",
                        "/remoteApi/modelPage");

        assertThat(parseBody(transport.jsonRequests().get(0)))
                .containsEntry("taskName", "例行巡视")
                .containsEntry("pointCodes", "P001,P002")
                .containsEntry("cycleMonth", "1,2,12")
                .containsEntry("cycleWeek", "1,3,5")
                .containsEntry("endAction", "1")
                .containsEntry("fixedStartTime", "2026-07-09 09:30:00")
                .containsEntry("cycleExecuteTime", "10:20:30")
                .containsEntry("intervalExecuteTime", "08:00:00")
                .containsEntry("invalidDailyStartTime", "12:00:00")
                .containsEntry("invalidDailyEndTime", "13:30:00");
        assertThat(parseBody(transport.jsonRequests().get(1)))
                .containsEntry("taskCodes", List.of("TM-001", "TM-002"));
        assertThat(parseBody(transport.jsonRequests().get(2)))
                .containsEntry("taskCode", "TM-001");
        assertThat(parseBody(transport.jsonRequests().get(3)))
                .containsEntry("taskName", "巡视")
                .containsEntry("patrolType", "200")
                .containsEntry("dogCode", "DOG-001")
                .containsEntry("priority", "4")
                .containsEntry("isEnable", 1)
                .containsEntry("pageNo", 1)
                .containsEntry("pageSize", 100)
                .doesNotContainKey("pageNum");
    }

    @Test
    void taskTemplateRequestsValidateRequiredFieldsLocally() {
        RecordingTransport transport = new RecordingTransport();
        TaskTemplateApi api = new DefaultTaskTemplateApi(newExecutor(transport));

        assertThatThrownBy(() -> api.create(TaskTemplateCreateRequest.builder()
                .pointCodes(List.of("P001"))
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("taskName");
        assertThatThrownBy(() -> api.create(TaskTemplateCreateRequest.builder()
                .taskName("任务")
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("pointCodes");
        assertThatThrownBy(() -> api.create(TaskTemplateCreateRequest.builder()
                .taskName("任务")
                .pointCodes(List.of(" "))
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("pointCodes");
        assertThatThrownBy(() -> api.deleteByCodes(TaskTemplateDeleteRequest.builder().taskCodes(List.of()).build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("taskCodes");
        assertThatThrownBy(() -> api.getByCode(TaskTemplateGetRequest.builder().taskCode(" ").build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("taskCode");
        assertThatThrownBy(() -> api.page(TaskTemplatePageRequest.builder().pageNo(0).build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("pageNo");

        assertThat(transport.jsonRequests()).isEmpty();
        assertThat(transport.downloadRequests()).isEmpty();
    }

    @Test
    void businessWriteMethodsDoNotRetryAndTimeoutCarriesUnknownResult() {
        RecordingTransport transport = new RecordingTransport().enqueueTimeout();
        TaskTemplateApi api = new DefaultTaskTemplateApi(newExecutor(transport));

        assertThatThrownBy(() -> api.create(TaskTemplateCreateRequest.builder()
                .taskName("任务")
                .pointCodes(List.of("P001"))
                .build()))
                .isInstanceOfSatisfying(StationOpenApiTimeoutException.class, ex -> {
                    assertThat(ex.resultUnknown()).isTrue();
                    assertThat(ex.path()).isEqualTo("/remoteApi/onlySaveTaskModel");
                    assertThat(ex.operation()).isEqualTo("taskTemplate.create");
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

    private static LinkedHashMap<String, Object> taskTemplateInfo(String taskCode, String taskName) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("taskCode", taskCode);
        body.put("taskName", taskName);
        body.put("executeType", 3);
        body.put("patrolType", "200");
        body.put("pointCodes", "P001,P002");
        body.put("pointCount", 2);
        body.put("dogCode", "DOG-001");
        body.put("priority", 4);
        body.put("endAction", "1");
        body.put("deviceLevel", 3);
        body.put("createTime", "2026-07-09 10:20:30");
        body.put("isEnable", "1");
        body.put("fixedStartTime", "2026-07-09 09:30:00");
        body.put("cycleMonth", "1,2,12");
        body.put("cycleWeek", "1,3,5");
        body.put("cycleExecuteTime", "10:20:30");
        body.put("cycleStartTime", "2026-07-10 00:00:00");
        body.put("cycleEndTime", "2026-08-10 23:59:59");
        body.put("intervalNumber", 2);
        body.put("intervalType", 1);
        body.put("intervalExecuteTime", "08:00:00");
        body.put("intervalStartTime", "2026-07-11 00:00:00");
        body.put("intervalEndTime", "2026-08-11 23:59:59");
        body.put("invalidStartTime", "2026-07-12 00:00:00");
        body.put("invalidEndTime", "2026-07-13 00:00:00");
        body.put("invalidDailyStartTime", "12:00:00");
        body.put("invalidDailyEndTime", "13:30:00");
        body.put("repeatable", "1");
        body.put("stationName", "杭州站");
        body.put("planTimeContent", "每天 10:20:30");
        body.put("unknownField", "ignored");
        return body;
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
        private final Queue<HttpResponse> jsonResponses = new ArrayDeque<>();
        private final Queue<RuntimeException> failures = new ArrayDeque<>();
        private final List<HttpRequest> jsonRequests = new ArrayList<>();
        private final List<HttpRequest> downloadRequests = new ArrayList<>();

        private RecordingTransport enqueueSuccess(Object data) {
            jsonResponses.add(new HttpResponse(200, Map.of(), JSON.writeBytes(successBody(data))));
            return this;
        }

        private RecordingTransport enqueueTimeout() {
            failures.add(new HttpTimeoutException("task template timeout"));
            return this;
        }

        @Override
        public HttpResponse postJson(HttpRequest request) {
            jsonRequests.add(request);
            if (!failures.isEmpty()) {
                throw failures.remove();
            }
            return jsonResponses.remove();
        }

        @Override
        public HttpResponse download(HttpRequest request, OutputStream outputStream) {
            downloadRequests.add(request);
            throw new AssertionError("task template APIs must use postJson, not download");
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
