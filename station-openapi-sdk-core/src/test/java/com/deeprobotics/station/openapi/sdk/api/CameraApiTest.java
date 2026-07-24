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
import com.deeprobotics.station.openapi.sdk.internal.DefaultCameraApi;
import com.deeprobotics.station.openapi.sdk.internal.OpenApiExecutor;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraResetRequest;
import java.io.OutputStream;
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
 * 摄像机 API 契约测试。
 *
 * <p>只验证 SDK 本地行为，不连接真实摄像机或平台服务。摄像机控制和复位都会影响现场设备，
 * 因此测试重点锁定 DEVICE_CONTROL intent、不可默认重试以及超时后的 resultUnknown 语义。</p>
 */
class CameraApiTest {

    private static final JsonCodec JSON = JsonCodec.defaultCodec();

    @Test
    void cameraEndpointsUseExpectedPathsAndOperationIntents() {
        Map<String, OperationIntent> endpointIntents = readEndpointDefinitions(DefaultCameraApi.class).stream()
                .collect(Collectors.toMap(EndpointDefinition::path, EndpointDefinition::intent));

        assertThat(endpointIntents)
                .containsEntry("/remoteApi/cameraCtrl", OperationIntent.DEVICE_CONTROL)
                .containsEntry("/remoteApi/ptzReset", OperationIntent.DEVICE_CONTROL)
                .hasSize(2);
    }

    @Test
    void cameraControlAndResetUsePostJsonAndPreserveWireBody() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess(null)
                .enqueueSuccess(null);
        CameraApi api = new DefaultCameraApi(newExecutor(transport));

        api.control(CameraControlRequest.builder()
                .cameraCode(" CAM-001 ")
                .ctrlType(CameraControlRequest.ControlType.PTZ)
                .command(CameraControlRequest.PtzCommand.LEFT_UP)
                .value(CameraControlRequest.SwitchValue.START)
                .build());
        api.resetPtz(CameraResetRequest.builder().cameraCode("CAM-001").build());

        assertThat(transport.downloadRequests()).isEmpty();
        assertThat(transport.jsonRequests().stream()
                .map(request -> request.url().getPath())
                .collect(Collectors.toList()))
                .containsExactly("/remoteApi/cameraCtrl", "/remoteApi/ptzReset");

        assertThat(parseBody(transport.jsonRequests().get(0)))
                .containsEntry("cameraCode", "CAM-001")
                .containsEntry("ctrlType", 3)
                .containsEntry("command", 5)
                .containsEntry("value", 1)
                .doesNotContainKey("channelNum");
        assertThat(parseBody(transport.jsonRequests().get(1)))
                .containsEntry("cameraCode", "CAM-001")
                .hasSize(1);
    }

    @Test
    void cameraControlSupportsVisibleLightInfraredRawCodesAndOptionalChannel() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess(null)
                .enqueueSuccess(null)
                .enqueueSuccess(null);
        CameraApi api = new DefaultCameraApi(newExecutor(transport));

        api.control(CameraControlRequest.builder()
                .cameraCode("CAM-001")
                .ctrlType(CameraControlRequest.ControlType.VISIBLE_LIGHT)
                .command(CameraControlRequest.LensCommand.ZOOM_IN)
                .value(CameraControlRequest.SwitchValue.STOP)
                .channelNum(1)
                .build());
        api.control(CameraControlRequest.builder()
                .cameraCode("CAM-002")
                .ctrlType(CameraControlRequest.ControlType.INFRARED)
                .command(CameraControlRequest.LensCommand.AUTO_FOCUS)
                .channelNum(2)
                .build());
        api.control(CameraControlRequest.builder()
                .cameraCode("CAM-003")
                .ctrlType(5)
                .command(8)
                .value(0)
                .build());

        assertThat(parseBody(transport.jsonRequests().get(0)))
                .containsEntry("ctrlType", 4)
                .containsEntry("command", 1)
                .containsEntry("value", 0)
                .containsEntry("channelNum", 1);
        assertThat(parseBody(transport.jsonRequests().get(1)))
                .containsEntry("ctrlType", 5)
                .containsEntry("command", 5)
                .containsEntry("channelNum", 2)
                .doesNotContainKey("value");
        assertThat(parseBody(transport.jsonRequests().get(2)))
                .containsEntry("ctrlType", 5)
                .containsEntry("command", 8)
                .containsEntry("value", 0);
    }

    @Test
    void cameraRequestsValidateRequiredFieldsAndRangesLocally() {
        RecordingTransport transport = new RecordingTransport();
        CameraApi api = new DefaultCameraApi(newExecutor(transport));

        assertThatThrownBy(() -> api.control(CameraControlRequest.builder()
                .ctrlType(CameraControlRequest.ControlType.PTZ)
                .command(CameraControlRequest.PtzCommand.UP)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("cameraCode");
        assertThatThrownBy(() -> api.control(CameraControlRequest.builder()
                .cameraCode("CAM-001")
                .command(CameraControlRequest.PtzCommand.UP)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("ctrlType");
        assertThatThrownBy(() -> api.control(CameraControlRequest.builder()
                .cameraCode("CAM-001")
                .ctrlType(CameraControlRequest.ControlType.PTZ)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("command");
        assertThatThrownBy(() -> api.control(CameraControlRequest.builder()
                .cameraCode("CAM-001")
                .ctrlType(0)
                .command(1)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("ctrlType");
        assertThatThrownBy(() -> api.control(CameraControlRequest.builder()
                .cameraCode("CAM-001")
                .ctrlType(CameraControlRequest.ControlType.PTZ)
                .command(0)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("command");
        assertThatThrownBy(() -> api.control(CameraControlRequest.builder()
                .cameraCode("CAM-001")
                .ctrlType(CameraControlRequest.ControlType.PTZ)
                .command(1)
                .value(-1)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("value");
        assertThatThrownBy(() -> api.control(CameraControlRequest.builder()
                .cameraCode("CAM-001")
                .ctrlType(CameraControlRequest.ControlType.PTZ)
                .command(1)
                .value(2)
                .build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("value");
        assertThatThrownBy(() -> api.resetPtz(CameraResetRequest.builder().cameraCode(" ").build()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("cameraCode");

        assertThat(transport.jsonRequests()).isEmpty();
        assertThat(transport.downloadRequests()).isEmpty();
    }

    @Test
    void rawCameraCodesRemainForwardCompatibleEscapeHatch() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueSuccess(null);
        CameraApi api = new DefaultCameraApi(newExecutor(transport));

        api.control(CameraControlRequest.builder()
                .cameraCode("CAM-001")
                .ctrlType(6)
                .command(9)
                .build());

        assertThat(parseBody(transport.jsonRequests().get(0)))
                .containsEntry("ctrlType", 6)
                .containsEntry("command", 9);
    }

    @Test
    void cameraDeviceControlDoesNotRetryAndTimeoutCarriesUnknownResult() {
        RecordingTransport transport = new RecordingTransport().enqueueTimeout();
        CameraApi api = new DefaultCameraApi(newExecutor(transport));

        assertThatThrownBy(() -> api.control(CameraControlRequest.builder()
                .cameraCode("CAM-001")
                .ctrlType(CameraControlRequest.ControlType.PTZ)
                .command(CameraControlRequest.PtzCommand.RIGHT)
                .value(CameraControlRequest.SwitchValue.START)
                .build()))
                .isInstanceOfSatisfying(StationOpenApiTimeoutException.class, ex -> {
                    assertThat(ex.resultUnknown()).isTrue();
                    assertThat(ex.path()).isEqualTo("/remoteApi/cameraCtrl");
                    assertThat(ex.operation()).isEqualTo("camera.control");
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
            failures.add(new HttpTimeoutException("camera timeout"));
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
            throw new AssertionError("camera APIs must use postJson, not download");
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
