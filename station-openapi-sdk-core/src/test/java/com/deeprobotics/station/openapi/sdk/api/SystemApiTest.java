package com.deeprobotics.station.openapi.sdk.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.auth.AccessTokenAuthProvider;
import com.deeprobotics.station.openapi.sdk.auth.Credential;
import com.deeprobotics.station.openapi.sdk.auth.SignatureAuthProvider;
import com.deeprobotics.station.openapi.sdk.internal.DefaultSystemApi;
import com.deeprobotics.station.openapi.sdk.internal.OpenApiExecutor;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.Test;

/**
 * 系统时间接口的鉴权请求形态测试。
 *
 * <p>{@code /remoteApi/time} 没有业务字段，但不能绕过统一执行链：签名模式必须发送纯鉴权 body，
 * Token 模式必须发送 {@code {}} 并携带 header。本测试专门防止维护者把“空业务请求”误改成无 body 或免鉴权。</p>
 */
class SystemApiTest {

    private static final JsonCodec JSON = JsonCodec.defaultCodec();

    @Test
    void systemEndpointUsesReadOnlyIntent() throws Exception {
        var field = DefaultSystemApi.class.getDeclaredField("GET_TIME_TEXT");
        field.setAccessible(true);
        EndpointDefinition endpoint = (EndpointDefinition) field.get(null);

        assertThat(endpoint.path()).isEqualTo("/remoteApi/time");
        assertThat(endpoint.intent()).isEqualTo(OperationIntent.READ_ONLY);
    }

    @Test
    void timeInSignatureModeSendsAuthOnlyBody() {
        RecordingTransport transport = new RecordingTransport().enqueueSuccess("2026-07-09T10:20:30");
        StationOpenApiConfig config = StationOpenApiConfig.builder()
                .endpoint("http://host:23288")
                .appKey("app-key")
                .secretKey("secret-key")
                .build();
        OpenApiExecutor executor = new OpenApiExecutor(
                config,
                new SignatureAuthProvider(
                        Credential.signature("app-key", "secret-key"),
                        () -> 1770601227007L,
                        () -> "HypPCn0tMJW549OMPNu32A3fyLSbsQ3w"),
                transport,
                JSON);
        SystemApi api = new DefaultSystemApi(executor);

        String timeText = api.getTimeText();

        assertThat(timeText).isEqualTo("2026-07-09T10:20:30");
        assertThat(transport.downloadRequests()).isEmpty();
        assertThat(transport.jsonRequests()).hasSize(1);
        assertThat(transport.jsonRequests().get(0).url().getPath()).isEqualTo("/remoteApi/time");

        Map<String, Object> body = parseBody(transport.jsonRequests().get(0));
        assertThat(body.keySet()).containsExactlyInAnyOrder("appKey", "timestamp", "nonce", "sign");
        assertThat(body)
                .containsEntry("appKey", "app-key")
                .containsEntry("timestamp", "1770601227007")
                .containsEntry("nonce", "HypPCn0tMJW549OMPNu32A3fyLSbsQ3w");
    }

    @Test
    void timeInTokenModeSendsEmptyJsonObjectWithAccessTokenHeader() {
        RecordingTransport transport = new RecordingTransport().enqueueSuccess("2026-07-09T10:20:30");
        StationOpenApiConfig config = StationOpenApiConfig.builder()
                .endpoint("http://host:23288")
                .accessToken("token-value")
                .build();
        OpenApiExecutor executor = new OpenApiExecutor(
                config,
                new AccessTokenAuthProvider(Credential.accessToken("token-value")),
                transport,
                JSON);
        SystemApi api = new DefaultSystemApi(executor);

        String timeText = api.getTimeText(RequestOptions.builder().requestId("system-req-1").build());

        assertThat(timeText).isEqualTo("2026-07-09T10:20:30");
        assertThat(transport.downloadRequests()).isEmpty();
        assertThat(transport.jsonRequests()).hasSize(1);
        assertThat(transport.jsonRequests().get(0).url().getPath()).isEqualTo("/remoteApi/time");
        assertThat(parseBody(transport.jsonRequests().get(0))).isEmpty();
        assertThat(transport.jsonRequests().get(0).headers())
                .containsEntry("X-Access-Token", "token-value")
                .containsEntry("X-Request-Id", "system-req-1");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseBody(HttpRequest request) {
        return JSON.read(request.body(), new TypeRef<LinkedHashMap<String, Object>>() {
        });
    }

    private static final class RecordingTransport implements HttpTransport {
        private final Queue<HttpResponse> jsonResponses = new ArrayDeque<>();
        private final List<HttpRequest> jsonRequests = new ArrayList<>();
        private final List<HttpRequest> downloadRequests = new ArrayList<>();

        private RecordingTransport enqueueSuccess(String data) {
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
            throw new AssertionError("system APIs must use postJson, not download");
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

        private static LinkedHashMap<String, Object> successBody(String data) {
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("code", 200);
            body.put("msg", "OK");
            body.put("data", data);
            return body;
        }
    }
}
