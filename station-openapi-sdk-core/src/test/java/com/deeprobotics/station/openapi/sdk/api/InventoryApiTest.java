package com.deeprobotics.station.openapi.sdk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.auth.AccessTokenAuthProvider;
import com.deeprobotics.station.openapi.sdk.auth.Credential;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.internal.DefaultInventoryApi;
import com.deeprobotics.station.openapi.sdk.internal.OpenApiExecutor;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DevicePointPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DockListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DogPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.MaintainAreaPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.MapListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.NodeListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.RoadMapListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.StreamPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.CameraStreamInfo;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.DogInfo;
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
 * 台账与基础资源 API 契约测试。
 *
 * <p>集中锁定八个 READ_ONLY endpoint、请求 wire 字段、分页兼容、敏感摄像机密码脱敏和 dogCode
 * 本地校验。内存 transport 返回平台 JSON 响应封装，因此可以同时验证 API 映射与 executor 解包，
 * 不依赖真实站点、数据库或设备。</p>
 */
class InventoryApiTest {

    private static final JsonCodec JSON = JsonCodec.defaultCodec();

    @Test
    void inventoryEndpointsUsePostReadOnlyIntentAndExpectedPaths() {
        Set<EndpointDefinition> endpoints = readEndpointDefinitions(DefaultInventoryApi.class);

        assertThat(endpoints)
                .extracting(EndpointDefinition::path)
                .containsExactlyInAnyOrder(
                        "/remoteApi/dogPage",
                        "/remoteApi/mapList",
                        "/remoteApi/streamPage",
                        "/remoteApi/pointPage",
                        "/remoteApi/maintainAreaPage",
                        "/remoteApi/roadMapList",
                        "/remoteApi/dockList",
                        "/remoteApi/nodeList");
        assertThat(endpoints)
                .extracting(EndpointDefinition::intent)
                .containsOnly(OperationIntent.READ_ONLY);
    }

    @Test
    void inventoryApiUsesPostJsonEndpointsParsesResponsesAndNeverEmitsPageNum() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess(page(
                        List.of(Map.of(
                                "dogName", "Alpha",
                                "dogCode", "DOG-001",
                                "deviceMode", "X30",
                                "dogTypeCode", "TYPE-1",
                                "productionCode", "PROD-1",
                                "ip", "10.0.0.1",
                                "port", "9000",
                                "floor", "F1")),
                        1L,
                        2L,
                        30L,
                        1L))
                .enqueueSuccess(List.of(Map.of(
                        "floor", "F1",
                        "dogCode", "DOG-001",
                        "mapFileName", "map.bin",
                        "twoDMapInfo", "{}",
                        "twoDMapBmpPath", "map.bmp")))
                .enqueueSuccess(linkedMapOf(
                        "records", List.of(linkedMapOf(
                                "name", "Visible Camera",
                                "code", "CAM-001",
                                "bindDeviceCode", "DOG-001",
                                "manufacturer", "HK",
                                "deviceMode", "CM-1",
                                "useMode", "12",
                                "videoMode", "1",
                                "ip", "10.0.0.2",
                                "port", "8000",
                                "username", "admin",
                                "password", "camera-secret",
                                "channel", "1",
                                "bitstream", "main",
                                "streamUrl", "rtsp://viewer:synthetic-stream-credential@camera/1")),
                        "total", 1L))
                .enqueueSuccess(page(
                        List.of(linkedMapOf(
                                "areaCode", "AREA-1",
                                "areaName", "Area 1",
                                "intervalCode", "INT-1",
                                "intervalName", "Interval 1",
                                "deviceCode", "DEV-1",
                                "deviceName", "Device 1",
                                "componentCode", "COMP-1",
                                "componentName", "Component 1",
                                "pointCode", "POINT-1",
                                "pointName", "Point 1",
                                "deviceType", "TYPE-A",
                                "recognitionType", "100",
                                "phase", "A",
                                "phaseGroupId", "PG-1",
                                "dogCode", "DOG-001",
                                "dockCode", "DOCK-1",
                                "floor", "F1")),
                        1L,
                        1L,
                        10L,
                        1L))
                .enqueueSuccess(page(
                        List.of(linkedMapOf(
                                "maintainAreaCode", "MA-1",
                                "startX", "1",
                                "startY", "2",
                                "endX", "3",
                                "endY", "4",
                                "startTime", "2026-07-09",
                                "endTime", "2026-07-10",
                                "repairDesc", "Area maintenance",
                                "dogCode", "DOG-001",
                                "issued", 1,
                                "pointCodes", "P1,P2",
                                "pointCount", 2,
                                "floor", "F1",
                                "mode", 2)),
                        1L,
                        1L,
                        10L,
                        1L))
                .enqueueSuccess(List.of(Map.of(
                        "dogCode", "DOG-001",
                        "edgeCode", "EDGE-1",
                        "edgeStartNode", "NODE-A",
                        "edgeEndNode", "NODE-B",
                        "startCoordinate", "1,2",
                        "endCoordinate", "3,4",
                        "controlPoint1", "5,6",
                        "controlPoint2", "7,8",
                        "floor", "F1",
                        "moveDisable", false)))
                .enqueueSuccess(List.of(Map.of(
                        "dockCode", "DOCK-1",
                        "dogCode", "DOG-001",
                        "edgeCode", "EDGE-1",
                        "floor", "F1",
                        "coordinate", "1,2",
                        "priority", "1",
                        "dockName", "Dock 1")))
                .enqueueSuccess(List.of(Map.of(
                        "nodeCode", "NODE-1",
                        "nodeName", "Node 1",
                        "dogCode", "DOG-001",
                        "floor", "F1",
                        "coordinate", "1,2")));

        InventoryApi api = new DefaultInventoryApi(newExecutor(transport));

        PageResult<DogInfo> dogs = api.pageDogs(
                DogPageRequest.builder().pageNo(2).pageSize(30).dogName("Alpha").deviceMode("X30").build(),
                RequestOptions.builder().requestId("inventory-req-1").build());
        api.listMaps(MapListRequest.builder().floor("F1").dogCode("DOG-001").build());
        PageResult<CameraStreamInfo> streams = api.pageStreams(
                StreamPageRequest.builder().pageNo(1).pageSize(20).bindDeviceCode("DOG-001").build());
        api.pagePoints(DevicePointPageRequest.builder().dogCode("DOG-001").pageNo(1).pageSize(10).build());
        api.pageMaintainAreas(MaintainAreaPageRequest.builder()
                .dogCode("DOG-001")
                .maintainAreaCode("MA-1")
                .repairDesc("Area maintenance")
                .startTime(LocalDateTime.of(2026, 7, 9, 10, 20, 30))
                .endTime(LocalDateTime.of(2026, 7, 9, 12, 20, 30))
                .issued(1)
                .floor("F1")
                .pageNo(1)
                .pageSize(10)
                .build());
        api.listRoadMaps(RoadMapListRequest.builder().dogCode("DOG-001").build());
        api.listDocks(DockListRequest.builder().dogCode("DOG-001").build());
        api.listNodes(NodeListRequest.builder().dogCode("DOG-001").build());

        assertThat(dogs.records()).hasSize(1);
        assertThat(dogs.pageNo()).isEqualTo(2L);
        assertThat(dogs.pageSize()).isEqualTo(30L);
        assertThat(streams.records()).hasSize(1);
        assertThat(streams.records().get(0).password()).isEqualTo("camera-secret");
        assertThat(streams.records().get(0).streamUrl())
                .isEqualTo("rtsp://viewer:synthetic-stream-credential@camera/1");
        assertThat(streams.pageNo()).isNull();
        assertThat(streams.pageSize()).isNull();
        assertThat(streams.pages()).isNull();
        assertThat(streams.total()).isEqualTo(1L);
        assertThat(streams.records().get(0).toString())
                .contains("password=<redacted>")
                .doesNotContain("synthetic-stream-credential")
                .doesNotContain("camera-secret");

        assertThat(transport.downloadRequests()).isEmpty();
        assertThat(transport.jsonRequests()).hasSize(8);
        assertThat(transport.jsonRequests().stream()
                .map(request -> request.url().getPath())
                .collect(Collectors.toList()))
                .containsExactly(
                        "/remoteApi/dogPage",
                        "/remoteApi/mapList",
                        "/remoteApi/streamPage",
                        "/remoteApi/pointPage",
                        "/remoteApi/maintainAreaPage",
                        "/remoteApi/roadMapList",
                        "/remoteApi/dockList",
                        "/remoteApi/nodeList");

        Map<String, Object> dogPageBody = parseBody(transport.jsonRequests().get(0));
        assertThat(dogPageBody)
                .containsEntry("pageNo", 2)
                .containsEntry("pageSize", 30)
                .containsEntry("dogName", "Alpha")
                .doesNotContainKey("pageNum");
    }

    @Test
    void inventoryListResponsesAreImmutable() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess(List.of(linkedMapOf("mapCode", "MAP-001")))
                .enqueueSuccess(List.of(linkedMapOf("edgeCode", "EDGE-001")))
                .enqueueSuccess(List.of(linkedMapOf("dockCode", "DOCK-001")))
                .enqueueSuccess(List.of(linkedMapOf("nodeCode", "NODE-001")));
        InventoryApi api = new DefaultInventoryApi(newExecutor(transport));

        List<List<?>> responses = List.of(
                api.listMaps(MapListRequest.builder().build()),
                api.listRoadMaps(RoadMapListRequest.builder().dogCode("DOG-001").build()),
                api.listDocks(DockListRequest.builder().dogCode("DOG-001").build()),
                api.listNodes(NodeListRequest.builder().dogCode("DOG-001").build()));

        for (List<?> response : responses) {
            assertThat(response).hasSize(1);
            assertThatThrownBy(response::clear)
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    void roadMapDockAndNodeRequestsValidateDogCodeLocally() {
        RecordingTransport transport = new RecordingTransport();
        InventoryApi api = new DefaultInventoryApi(newExecutor(transport));

        assertThatThrownBy(() -> api.listRoadMaps(RoadMapListRequest.builder().dogCode(" ").build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("dogCode");
        assertThatThrownBy(() -> api.listDocks(DockListRequest.builder().dogCode("").build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("dogCode");
        assertThatThrownBy(() -> api.listNodes(NodeListRequest.builder().dogCode(null).build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("dogCode");

        assertThat(transport.jsonRequests()).isEmpty();
        assertThat(transport.downloadRequests()).isEmpty();
    }

    @Test
    void inventoryRequestsRejectNullRequestObjectsLocally() {
        RecordingTransport transport = new RecordingTransport();
        InventoryApi api = new DefaultInventoryApi(newExecutor(transport));

        assertThatThrownBy(() -> api.pageDogs(null))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("request");
        assertThatThrownBy(() -> api.listMaps(null))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("request");
        assertThatThrownBy(() -> api.pageStreams(null))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("request");
        assertThatThrownBy(() -> api.pagePoints(null))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("request");
        assertThatThrownBy(() -> api.pageMaintainAreas(null))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("request");
        assertThatThrownBy(() -> api.listRoadMaps(null))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("request");
        assertThatThrownBy(() -> api.listDocks(null))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("request");
        assertThatThrownBy(() -> api.listNodes(null))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("request");

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

    @SuppressWarnings("unchecked")
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
            throw new AssertionError("inventory APIs must use postJson, not download");
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
