package com.deeprobotics.station.openapi.sdk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.auth.AuthProvider;
import com.deeprobotics.station.openapi.sdk.auth.AuthenticatedRequest;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiBusinessException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.internal.DefaultResultApi;
import com.deeprobotics.station.openapi.sdk.internal.OpenApiExecutor;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.request.result.ResourceDownloadRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.Test;

/**
 * 资源流式下载 API 契约测试。
 *
 * <p>{@code /remoteApi/result/resource} 和普通 JSON 接口不同：成功响应是二进制流，失败响应可能是
 * JSON {@code R<?>}。正常二进制直接写出，JSON 失败转业务异常。</p>
 */
class ResourceDownloadTest {

    private static final JsonCodec JSON = JsonCodec.defaultCodec();

    @Test
    void downloadResourceToOutputStreamUsesDownloadTransportAndDoesNotUnwrapBinarySuccess() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadBytes("binary-file".getBytes(StandardCharsets.UTF_8));
        ResultApi api = new DefaultResultApi(newExecutor(transport, new PassthroughAuthProvider()));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        api.downloadResource(ResourceDownloadRequest.builder()
                .filePath(" map/floor1.bin ")
                .type(ResourceDownloadRequest.Type.MAP_FILE)
                .build(), output);

        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("binary-file");
        assertThat(transport.jsonRequests()).isEmpty();
        assertThat(transport.downloadRequests()).hasSize(1);
        assertThat(transport.downloadRequests().get(0).url().getPath()).isEqualTo("/remoteApi/result/resource");
        assertThat(parseBody(transport.downloadRequests().get(0)))
                .containsEntry("filePath", "map/floor1.bin")
                .containsEntry("type", 1);
    }

    @Test
    void jsonDownloadFailureIsParsedAsBusinessExceptionAndDoesNotWriteOutputStream() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadResponse(200, Map.of("Content-Type", List.of("application/json; charset=utf-8")),
                        failureBody(40003, "download permission denied"));
        ResultApi api = new DefaultResultApi(newExecutor(transport, new PassthroughAuthProvider()));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThatThrownBy(() -> api.downloadResource(ResourceDownloadRequest.builder()
                .filePath("result/TE-001/P001.jpg")
                .type(2)
                .build(), output, RequestOptions.builder().requestId("download-json-failure").build()))
                .isInstanceOf(StationOpenApiBusinessException.class)
                .hasMessageContaining("code=40003")
                .hasMessageContaining("download permission denied")
                .hasMessageContaining("/remoteApi/result/resource")
                .hasMessageContaining("download-json-failure");
        assertThat(output.size()).isZero();
    }

    @Test
    void fileDownloadAuthenticatesBusinessBody() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadBytes("signed-file".getBytes(StandardCharsets.UTF_8));
        SequencedAuthProvider authProvider = new SequencedAuthProvider();
        ResultApi api = new DefaultResultApi(newExecutor(transport, authProvider));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        api.downloadResource(ResourceDownloadRequest.builder()
                .filePath("result/signed.jpg")
                .type(2)
                .build(), output);

        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("signed-file");
        assertThat(transport.downloadRequests()).hasSize(1);
        assertThat(authProvider.calls()).isEqualTo(1);
        Map<String, Object> firstBody = parseBody(transport.downloadRequests().get(0));
        assertThat(firstBody).containsEntry("nonce", "nonce-1").containsEntry("sign", "sign-1");
    }

    @Test
    void resourceDownloadRequestValidatesFilePathAndTypeLocally() {
        RecordingTransport transport = new RecordingTransport();
        ResultApi api = new DefaultResultApi(newExecutor(transport, new PassthroughAuthProvider()));

        assertThatThrownBy(() -> api.downloadResource(ResourceDownloadRequest.builder()
                .filePath(" ")
                .type(1)
                .build(), new ByteArrayOutputStream()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("filePath");
        assertThatThrownBy(() -> api.downloadResource(ResourceDownloadRequest.builder()
                .filePath("result/TE-001/P001.jpg")
                .type(3)
                .build(), new ByteArrayOutputStream()))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("type");

        assertThat(transport.jsonRequests()).isEmpty();
        assertThat(transport.downloadRequests()).isEmpty();
    }

    private static OpenApiExecutor newExecutor(RecordingTransport transport, AuthProvider authProvider) {
        StationOpenApiConfig config = StationOpenApiConfig.builder()
                .endpoint("http://host:23288")
                .accessToken("token-value")
                .build();
        return new OpenApiExecutor(config, authProvider, transport, JSON);
    }

    private static Map<String, Object> parseBody(HttpRequest request) {
        return JSON.read(request.body(), new TypeRef<LinkedHashMap<String, Object>>() {
        });
    }

    private static LinkedHashMap<String, Object> failureBody(int code, String message) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", code);
        body.put("msg", message);
        body.put("data", null);
        return body;
    }

    private static final class PassthroughAuthProvider implements AuthProvider {
        @Override
        public AuthenticatedRequest authenticate(Map<String, Object> businessBody) {
            return new AuthenticatedRequest(MapUtils.copyWithoutNulls(businessBody), Map.of());
        }
    }

    private static final class SequencedAuthProvider implements AuthProvider {
        private int calls;

        @Override
        public AuthenticatedRequest authenticate(Map<String, Object> businessBody) {
            calls++;
            LinkedHashMap<String, Object> body = MapUtils.copyWithoutNulls(businessBody);
            body.put("appKey", "app");
            body.put("timestamp", "177060122700" + calls);
            body.put("nonce", "nonce-" + calls);
            body.put("sign", "sign-" + calls);
            return new AuthenticatedRequest(body, Map.of("X-Auth-Attempt", String.valueOf(calls)));
        }

        private int calls() {
            return calls;
        }
    }

    private static final class RecordingTransport implements HttpTransport {
        private final Queue<Object> downloadOutcomes = new ArrayDeque<>();
        private final List<HttpRequest> jsonRequests = new ArrayList<>();
        private final List<HttpRequest> downloadRequests = new ArrayList<>();

        private RecordingTransport enqueueDownloadBytes(byte[] bytes) {
            downloadOutcomes.add(bytes);
            return this;
        }

        private RecordingTransport enqueueDownloadResponse(int statusCode, Map<String, List<String>> headers,
                Object responseBody) {
            downloadOutcomes.add(new HttpResponse(statusCode, headers, JSON.writeBytes(responseBody)));
            return this;
        }

        @Override
        public HttpResponse postJson(HttpRequest request) {
            jsonRequests.add(request);
            throw new AssertionError("resource download API must use download, not postJson");
        }

        @Override
        public HttpResponse download(HttpRequest request, OutputStream outputStream) {
            downloadRequests.add(request);
            Object outcome = downloadOutcomes.remove();
            if (outcome instanceof HttpResponse response) {
                return response;
            }
            try {
                outputStream.write((byte[]) outcome);
            } catch (IOException ex) {
                throw new AssertionError(ex);
            }
            return new HttpResponse(200, Map.of("Content-Type", List.of("application/octet-stream")), new byte[0]);
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
    }
}
