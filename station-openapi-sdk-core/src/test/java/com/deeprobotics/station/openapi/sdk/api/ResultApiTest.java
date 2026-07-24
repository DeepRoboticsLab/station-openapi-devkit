package com.deeprobotics.station.openapi.sdk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.auth.AccessTokenAuthProvider;
import com.deeprobotics.station.openapi.sdk.auth.Credential;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.internal.DefaultResultApi;
import com.deeprobotics.station.openapi.sdk.internal.OpenApiExecutor;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.FileInfo;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.result.TaskPointResultPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.result.PatrolResultInfo;
import java.io.OutputStream;
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
 * 巡检结果查询 API 契约测试。
 *
 * <p>本测试只覆盖结果分页查询本身：它必须走普通 JSON POST、标记为 READ_ONLY，并把平台
 * 响应 {@code fileList} 中的 {@code filePath/title/fileType} 映射到 SDK 公共文件模型。
 * 文件流下载的协议差异由 {@link ResourceDownloadTest} 独立覆盖。</p>
 */
class ResultApiTest {

    private static final JsonCodec JSON = JsonCodec.defaultCodec();

    @Test
    void resultEndpointsUseExpectedPathsAndOperationIntents() {
        Map<String, OperationIntent> endpointIntents = readEndpointDefinitions(DefaultResultApi.class).stream()
                .collect(Collectors.toMap(EndpointDefinition::path, EndpointDefinition::intent));

        assertThat(endpointIntents)
                .containsEntry("/remoteApi/taskPointResultPage", OperationIntent.READ_ONLY)
                .containsEntry("/remoteApi/result/resource", OperationIntent.FILE_DOWNLOAD)
                .hasSize(2);
    }

    @Test
    void pageTaskPointResultsUsesPostJsonParsesPageAndMapsFileList() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess(page(List.of(patrolResultInfo()), 1L, 1L, 10L, 1L));
        ResultApi api = new DefaultResultApi(newExecutor(transport));

        PageResult<PatrolResultInfo> page = api.pageTaskPointResults(TaskPointResultPageRequest.builder()
                .taskExecuteCode(" TE-001 ")
                .pageNo(1)
                .pageSize(200)
                .build());

        assertThat(page.records()).hasSize(1);
        PatrolResultInfo result = page.records().get(0);
        assertThat(result.taskExecuteCode()).isEqualTo("TE-001");
        assertThat(result.devicePointCode()).isEqualTo("P001");
        assertThat(result.patrolTime()).isEqualTo(LocalDateTime.of(2026, 7, 9, 9, 30, 0));
        assertThat(result.confidence()).isEqualTo("0.86");
        assertThat(result.fileList())
                .containsExactly(new FileInfo("result/TE-001/P001-visible.jpg", "可见光照片", "2"));
        assertThat(transport.downloadRequests()).isEmpty();
        assertThat(transport.jsonRequests().stream()
                .map(request -> request.url().getPath())
                .collect(Collectors.toList()))
                .containsExactly("/remoteApi/taskPointResultPage");
        assertThat(parseBody(transport.jsonRequests().get(0)))
                .containsEntry("taskExecuteCode", "TE-001")
                .containsEntry("pageNo", 1)
                .containsEntry("pageSize", 100)
                .doesNotContainKey("pageNum");
    }

    @Test
    void pageTaskPointResultsAllowsMissingConfidence() {
        LinkedHashMap<String, Object> responseRecord = patrolResultInfo();
        responseRecord.remove("confidence");
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess(page(List.of(responseRecord), 1L, 1L, 10L, 1L));
        ResultApi api = new DefaultResultApi(newExecutor(transport));

        PageResult<PatrolResultInfo> page = api.pageTaskPointResults(TaskPointResultPageRequest.builder()
                .taskExecuteCode("TE-001")
                .build());

        assertThat(page.records().get(0).confidence()).isNull();
    }

    @Test
    void taskPointResultPageRequestRequiresTaskExecuteCode() {
        RecordingTransport transport = new RecordingTransport();
        ResultApi api = new DefaultResultApi(newExecutor(transport));

        assertThatThrownBy(() -> api.pageTaskPointResults(TaskPointResultPageRequest.builder()
                .taskExecuteCode(" ")
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("taskExecuteCode");
        assertThatThrownBy(() -> api.pageTaskPointResults(TaskPointResultPageRequest.builder()
                .taskExecuteCode("TE-001")
                .pageNo(0)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("pageNo");

        assertThat(transport.jsonRequests()).isEmpty();
        assertThat(transport.downloadRequests()).isEmpty();
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

    private static LinkedHashMap<String, Object> patrolResultInfo() {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("areaCode", "A001");
        body.put("areaName", "主变区域");
        body.put("intervalCode", "I001");
        body.put("intervalName", "间隔一");
        body.put("deviceCode", "D001");
        body.put("deviceName", "主设备");
        body.put("componentCode", "C001");
        body.put("componentName", "套管");
        body.put("devicePointCode", "P001");
        body.put("devicePointName", "油温表");
        body.put("taskCode", "TM-001");
        body.put("taskName", "例行巡视");
        body.put("taskExecuteCode", "TE-001");
        body.put("recognitionType", "meter");
        body.put("deviceType", "transformer");
        body.put("phase", "A");
        body.put("phaseGroupId", "PG-1");
        body.put("dogCode", "DOG-001");
        body.put("patrolTime", "2026-07-09 09:30:00");
        body.put("patrolValue", "35.6");
        body.put("patrolUnit", "C");
        body.put("fileList", List.of(linkedMapOf(
                "filePath", "result/TE-001/P001-visible.jpg",
                "title", "可见光照片",
                "fileType", "2",
                "unknownField", "ignored")));
        body.put("alarmLevel", 1);
        body.put("alarmType", "temperature");
        body.put("alarmContent", "温度偏高");
        body.put("valid", 1);
        body.put("originalValue", "35.600");
        body.put("confidence", "0.86");
        body.put("unknownField", "ignored");
        return body;
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
        private final Queue<HttpResponse> jsonResponses = new ArrayDeque<>();
        private final List<HttpRequest> jsonRequests = new ArrayList<>();
        private final List<HttpRequest> downloadRequests = new ArrayList<>();

        private RecordingTransport enqueueSuccess(Object data) {
            jsonResponses.add(new HttpResponse(200, Map.of(), JSON.writeBytes(successBody(data))));
            return this;
        }

        @Override
        public HttpResponse postJson(HttpRequest request) {
            jsonRequests.add(request);
            return jsonResponses.remove();
        }

        @Override
        public HttpResponse download(HttpRequest request, OutputStream outputStream) {
            downloadRequests.add(request);
            throw new AssertionError("result page API must use postJson, not download");
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
