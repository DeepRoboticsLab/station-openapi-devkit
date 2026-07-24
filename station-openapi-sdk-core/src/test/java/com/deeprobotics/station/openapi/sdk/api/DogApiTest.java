package com.deeprobotics.station.openapi.sdk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.auth.AccessTokenAuthProvider;
import com.deeprobotics.station.openapi.sdk.auth.Credential;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTimeoutException;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTimeoutException;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.internal.DefaultDogApi;
import com.deeprobotics.station.openapi.sdk.internal.OpenApiExecutor;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogAlarmPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogChargeControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogPositionRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogPositionSetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogStateRequest;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogAlarmInfo;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogPosition;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogState;
import java.io.OutputStream;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 四足狗 API 契约测试。
 *
 * <p>这些测试不连接真实平台、四足狗或摄像头，只通过内存中的 {@link RecordingTransport}
 * 验证 SDK 对外契约：RemoteApi path、操作意图、JSON body 形态、本地校验、重试语义和 {@code dogPos}
 * 二次反序列化。这样可以在没有现场设备的 CI 环境中稳定覆盖客户最容易踩错的调用链。</p>
 */
class DogApiTest {

    private static final JsonCodec JSON = JsonCodec.defaultCodec();

    @Test
    void dogEndpointsUseExpectedPathsAndOperationIntents() {
        // EndpointDefinition 是执行器判断重试策略和异常语义的依据；路径和 intent 必须一起锁定。
        Map<String, OperationIntent> endpointIntents = readEndpointDefinitions(DefaultDogApi.class).stream()
                .collect(Collectors.toMap(EndpointDefinition::path, EndpointDefinition::intent));

        assertThat(endpointIntents)
                .containsEntry("/remoteApi/getDogStateData", OperationIntent.READ_ONLY)
                .containsEntry("/remoteApi/chargeCtrl", OperationIntent.DEVICE_CONTROL)
                .containsEntry("/remoteApi/controlDog", OperationIntent.DEVICE_CONTROL)
                .containsEntry("/remoteApi/setPosToDog", OperationIntent.DEVICE_CONTROL)
                .containsEntry("/remoteApi/alarmPage", OperationIntent.READ_ONLY)
                .containsEntry("/remoteApi/dogPos", OperationIntent.READ_ONLY)
                .hasSize(6);
    }

    @Test
    void dogApiUsesPostJsonEndpointsParsesResponsesAndPreservesWireShapes() {
        // 按业务调用顺序排队响应，模拟平台响应 data，并在后面检查 SDK 发出的每个 JSON body。
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess(dogStateResponse())
                .enqueueSuccess(null)
                .enqueueSuccess(null)
                .enqueueSuccess(null)
                .enqueueSuccess(page(List.of(linkedMapOf(
                        "dogName", "X30-1",
                        "dogCode", "DOG-001",
                        "alarmTime", "2026-07-09 10:20:30",
                        "alarmCode", "ALM-001",
                        "alarmContent", "low battery")), 1L, 1L, 10L, 1L))
                .enqueueSuccess("{\"dogCode\":\"001\",\"dogName\":\"X30-1\",\"x\":\"-643.23\","
                        + "\"y\":\"1329.13\",\"yaw\":\"1.53\",\"z\":\"0.00\","
                        + "\"time\":\"2026-02-26 20:24:32.932\",\"unknownField\":\"ignored\"}");
        DogApi api = new DefaultDogApi(newExecutor(transport));

        // 请求侧刻意混入首尾空格，验证请求模型会在生成 wire body 前 trim dogCode。
        DogState state = api.getState(DogStateRequest.builder().dogCode(" DOG-001 ").build(),
                RequestOptions.builder().requestId("dog-state-req").build());
        api.chargeControl(DogChargeControlRequest.builder()
                .dogCode("DOG-001")
                .command(DogChargeControlRequest.Command.RETURN_TO_CHARGE)
                .build());
        api.control(DogControlRequest.builder()
                .dogCode("DOG-001")
                .type(DogControlRequest.Type.MOTION_CONTROL)
                .command(DogControlRequest.MotionCommand.TURN_RIGHT)
                .build());
        api.setPosition(DogPositionSetRequest.builder()
                .dogCode("DOG-001")
                .edgeCode("EDGE-001")
                .x("-643.23")
                .y("1329.13")
                .build());
        PageResult<DogAlarmInfo> alarms = api.pageAlarms(DogAlarmPageRequest.builder()
                .dogCode("DOG-001")
                .alarmCode("ALM-001")
                .startTime(LocalDateTime.of(2026, 7, 9, 10, 0, 0))
                .endTime(LocalDateTime.of(2026, 7, 9, 12, 0, 0))
                .pageNo(1)
                .pageSize(10)
                .build());
        Optional<DogPosition> position = api.getPosition(DogPositionRequest.builder().dogCode("DOG-001").build());

        assertThat(state.dogCode()).isEqualTo("DOG-001");
        assertThat(state.dogModel()).isEqualTo("X30");
        assertThat(state.isCharging()).isTrue();
        assertThat(state.props()).hasSize(1);
        assertThat(state.props().get(0).groupName()).isEqualTo("batteryData.title");
        assertThat(state.props().get(0).list().get(0).status()).isEqualTo("batteryData.chargeState.title");
        assertThat(state.props().get(0).list().get(0).value()).isEqualTo("1");
        assertThat(state.props().get(0).list().get(0).remark()).isEqualTo("1=charging,0=idle");
        assertThat(state.props().get(0).list().get(0).alarmCondition()).isEqualTo("==1");
        assertThat(state.props().get(0).list().get(0).alarm()).isFalse();
        assertThat(state.props().get(0).list().get(0).unit()).isEqualTo("%");
        assertThat(alarms.records()).hasSize(1);
        assertThat(alarms.records().get(0).alarmTime())
                .isEqualTo(LocalDateTime.of(2026, 7, 9, 10, 20, 30));
        assertThat(position).contains(new DogPosition("001", "X30-1", "-643.23", "1329.13", "0.00", "1.53",
                "2026-02-26 20:24:32.932"));

        assertThat(transport.downloadRequests()).isEmpty();
        // 四足狗分组的六个接口都是 POST JSON；文件下载接口不应被该分组误用。
        assertThat(transport.jsonRequests().stream()
                .map(request -> request.url().getPath())
                .collect(Collectors.toList()))
                .containsExactly(
                        "/remoteApi/getDogStateData",
                        "/remoteApi/chargeCtrl",
                        "/remoteApi/controlDog",
                        "/remoteApi/setPosToDog",
                        "/remoteApi/alarmPage",
                        "/remoteApi/dogPos");

        assertThat(parseBody(transport.jsonRequests().get(0))).containsEntry("dogCode", "DOG-001");
        // chargeCtrl 的 command 必须是字符串 "1"/"2"，不能因为枚举实现变成数字 1/2。
        assertThat(parseBody(transport.jsonRequests().get(1)))
                .containsEntry("dogCode", "DOG-001")
                .containsEntry("command", "1");
        assertThat(parseBody(transport.jsonRequests().get(2)))
                .containsEntry("dogCode", "DOG-001")
                .containsEntry("type", 2)
                .containsEntry("command", 4)
                .containsEntry("value", "0")
                .doesNotContainKey("speed");
        // setPosToDog 的 x/y 是字符串坐标，z 不传时不能出现在 body 中。
        assertThat(parseBody(transport.jsonRequests().get(3)))
                .containsEntry("dogCode", "DOG-001")
                .containsEntry("edgeCode", "EDGE-001")
                .containsEntry("x", "-643.23")
                .containsEntry("y", "1329.13")
                .doesNotContainKey("z");
        assertThat(parseBody(transport.jsonRequests().get(4)))
                .containsEntry("startTime", "2026-07-09 10:00:00")
                .containsEntry("endTime", "2026-07-09 12:00:00")
                .containsEntry("pageNo", 1)
                .containsEntry("pageSize", 10)
                .doesNotContainKey("pageNum");
    }

    @Test
    void dogRequestsValidateRequiredFieldsSpeedAndMotionValueLocally() {
        RecordingTransport transport = new RecordingTransport();
        DogApi api = new DefaultDogApi(newExecutor(transport));

        // 所有非法入参都应在本地失败；如果 RecordingTransport 收到请求，说明校验链路漏掉了。
        assertThatThrownBy(() -> api.getState(DogStateRequest.builder().dogCode(" ").build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("dogCode");
        assertThatThrownBy(() -> api.chargeControl(DogChargeControlRequest.builder()
                .dogCode("DOG-001")
                .command((String) null)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("command");
        assertThatThrownBy(() -> api.control(DogControlRequest.builder()
                .type(DogControlRequest.Type.MOTION_CONTROL)
                .command(DogControlRequest.MotionCommand.FORWARD)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("dogCode");
        assertThatThrownBy(() -> api.control(DogControlRequest.builder()
                .dogCode("DOG-001")
                .command(DogControlRequest.MotionCommand.FORWARD)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("type");
        assertThatThrownBy(() -> api.control(DogControlRequest.builder()
                .dogCode("DOG-001")
                .type(DogControlRequest.Type.MOTION_CONTROL)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("command");
        assertThatThrownBy(() -> api.control(DogControlRequest.builder()
                .dogCode("DOG-001")
                .type(DogControlRequest.Type.MOTION_CONTROL)
                .command(DogControlRequest.MotionCommand.FORWARD)
                .value(" ")
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("value");
        assertThatThrownBy(() -> api.control(DogControlRequest.builder()
                .dogCode("DOG-001")
                .type(DogControlRequest.Type.MOTION_CONTROL)
                .command(DogControlRequest.MotionCommand.FORWARD)
                .speed(0)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("speed");
        assertThatThrownBy(() -> api.control(DogControlRequest.builder()
                .dogCode("DOG-001")
                .type(DogControlRequest.Type.MOTION_CONTROL)
                .command(DogControlRequest.MotionCommand.FORWARD)
                .speed(6)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("speed");
        assertThatThrownBy(() -> api.control(DogControlRequest.builder()
                .dogCode("DOG-001")
                .type(DogControlRequest.Type.MOTION_CONTROL)
                .command(DogControlRequest.MotionCommand.FORWARD)
                .value("fast")
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("value");
        assertThatThrownBy(() -> api.control(DogControlRequest.builder()
                .dogCode("DOG-001")
                .type(DogControlRequest.Type.MOTION_CONTROL)
                .command(DogControlRequest.MotionCommand.FORWARD)
                .value("0.7")
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("0.6");
        assertThatThrownBy(() -> api.setPosition(DogPositionSetRequest.builder()
                .dogCode("DOG-001")
                .edgeCode("EDGE-001")
                .x("")
                .y("1329.13")
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("x");
        assertThatThrownBy(() -> api.setPosition(DogPositionSetRequest.builder()
                .dogCode("DOG-001")
                .edgeCode("EDGE-001")
                .x("-643.23")
                .y(" ")
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("y");

        assertThat(transport.jsonRequests()).isEmpty();
        assertThat(transport.downloadRequests()).isEmpty();
    }

    @Test
    void dogControlSendsValidSpeedWhenPresent() {
        RecordingTransport transport = new RecordingTransport().enqueueSuccess(null);
        DogApi api = new DefaultDogApi(newExecutor(transport));

        api.control(DogControlRequest.builder()
                .dogCode("DOG-001")
                .type(DogControlRequest.Type.MOTION_CONTROL)
                .command(DogControlRequest.MotionCommand.FORWARD)
                .value("0.6")
                .speed(5)
                .build());

        assertThat(transport.jsonRequests()).hasSize(1);
        assertThat(parseBody(transport.jsonRequests().get(0)))
                .containsEntry("dogCode", "DOG-001")
                .containsEntry("type", 2)
                .containsEntry("command", 1)
                .containsEntry("value", "0.6")
                .containsEntry("speed", 5);
    }

    @Test
    void deviceControlMethodsDoNotRetryAndTimeoutCarriesUnknownResult() {
        RecordingTransport transport = new RecordingTransport().enqueueTimeout();
        DogApi api = new DefaultDogApi(newExecutor(transport));

        // 设备控制类接口读超时时，现场可能已经执行指令，因此异常必须带 resultUnknown=true。
        assertThatThrownBy(() -> api.chargeControl(DogChargeControlRequest.builder()
                .dogCode("DOG-001")
                .command(DogChargeControlRequest.Command.CANCEL_RETURN)
                .build()))
                .isInstanceOfSatisfying(StationOpenApiTimeoutException.class, ex -> {
                    assertThat(ex.resultUnknown()).isTrue();
                    assertThat(ex.path()).isEqualTo("/remoteApi/chargeCtrl");
                    assertThat(ex.operation()).isEqualTo("dog.chargeControl");
                });

        assertThat(transport.jsonRequests()).hasSize(1);
        assertThat(parseBody(transport.jsonRequests().get(0))).containsEntry("command", "2");
    }

    @Test
    void dogPositionMapsEmptyDataToOptionalParsesJsonAndRejectsMalformedJson() {
        // dogPos 成功响应的 data 可能是 null、空串、空白串、JSON 字符串或异常数据。
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess(null)
                .enqueueSuccess("")
                .enqueueSuccess("   ")
                .enqueueSuccess("{\"dogCode\":\"001\",\"dogName\":\"X30-1\",\"x\":\"-643.23\","
                        + "\"y\":\"1329.13\",\"z\":\"0.00\",\"yaw\":\"1.53\","
                        + "\"time\":\"2026-02-26 20:24:32.932\",\"taskPatrolledId\":\"must-not-leak\"}")
                .enqueueSuccess("not-json");
        DogApi api = new DefaultDogApi(newExecutor(transport));
        DogPositionRequest request = DogPositionRequest.builder().dogCode("DOG-001").build();

        assertThat(api.getPosition(request)).isEmpty();
        assertThat(api.getPosition(request)).isEmpty();
        assertThat(api.getPosition(request)).isEmpty();
        assertThat(api.getPosition(request)).contains(new DogPosition("001", "X30-1", "-643.23", "1329.13",
                "0.00", "1.53", "2026-02-26 20:24:32.932"));
        assertThatThrownBy(() -> api.getPosition(request))
                .isInstanceOf(StationOpenApiSerializationException.class)
                .hasMessageContaining("dogPos");

        // 公共模型只暴露稳定坐标字段，避免调用方依赖未公开字段或原始 JSON 字符串。
        assertThat(List.of(DogPosition.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .containsExactly("dogCode", "dogName", "x", "y", "z", "yaw", "time");
        assertThat(List.of(DogPosition.class.getDeclaredFields()))
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("taskPatrolledId");
    }

    @Test
    void dogPositionRejectsEveryNonObjectJsonRootWithSdkContext() {
        DogPositionRequest request = DogPositionRequest.builder().dogCode("DOG-001").build();

        for (String rawData : List.of("null", "\"position\"", "[]", "1")) {
            DogApi api = new DefaultDogApi(newExecutor(new RecordingTransport().enqueueSuccess(rawData)));

            assertThatThrownBy(() -> api.getPosition(request,
                    RequestOptions.builder().requestId("request-dog-pos-root").build()))
                    .as("dogPos root must be an object: %s", rawData)
                    .isInstanceOf(StationOpenApiSerializationException.class)
                    .hasMessageContaining("/remoteApi/dogPos")
                    .hasMessageContaining("dog.getPosition")
                    .hasMessageContaining("request-dog-pos-root");
        }
    }

    @Test
    void generatedRequestIdIsReusedByDogPositionPostProcessingFailure() {
        RecordingTransport transport = new RecordingTransport().enqueueSuccess("not-json");
        DogApi api = new DefaultDogApi(newExecutor(transport));
        DogPositionRequest request = DogPositionRequest.builder().dogCode("DOG-001").build();

        assertThatThrownBy(() -> api.getPosition(request))
                .isInstanceOfSatisfying(StationOpenApiSerializationException.class, failure -> {
                    String headerRequestId = transport.jsonRequests().get(0).headers().get("X-Request-Id");
                    assertThat(headerRequestId).isNotBlank();
                    assertThat(failure.requestId()).isEqualTo(headerRequestId);
                    assertThat(failure.path()).isEqualTo("/remoteApi/dogPos");
                    assertThat(failure.operation()).isEqualTo("dog.getPosition");
                });
    }

    private static OpenApiExecutor newExecutor(RecordingTransport transport) {
        // 测试使用 token 鉴权，避免签名字段干扰 body 形态断言；签名链路由 AuthProviderTest 覆盖。
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

    private static LinkedHashMap<String, Object> dogStateResponse() {
        // 模拟 ResultTransferUtils.transferDofProps 输出：保留 groupName/status/value 等原始 key。
        return linkedMapOf(
                "dogCode", "DOG-001",
                "dogModel", "X30",
                "props", List.of(linkedMapOf(
                        "groupName", "batteryData.title",
                        "list", List.of(linkedMapOf(
                                "status", "batteryData.chargeState.title",
                                "value", "1",
                                "remark", "1=charging,0=idle",
                                "alarmCondition", "==1",
                                "alarm", false,
                                "unit", "%")))));
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
        // DefaultDogApi 的 EndpointDefinition 是 private 常量，用反射读取能避免为了测试扩大生产可见性。
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
            // OpenApiExecutor 期望 transport 返回完整 R<T> JSON，data 才是各 API 的业务响应。
            outcomes.add(new HttpResponse(200, Map.of(), JSON.writeBytes(successBody(data))));
            return this;
        }

        private RecordingTransport enqueueTimeout() {
            outcomes.add(new HttpTimeoutException("read timed out"));
            return this;
        }

        @Override
        public HttpResponse postJson(HttpRequest request) {
            // 记录真实请求对象，后续断言 URL path、body 和 download 调用隔离。
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
            throw new AssertionError("dog APIs must use postJson, not download");
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
