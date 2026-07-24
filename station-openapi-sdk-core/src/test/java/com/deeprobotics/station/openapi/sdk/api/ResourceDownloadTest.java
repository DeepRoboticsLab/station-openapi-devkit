package com.deeprobotics.station.openapi.sdk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.auth.AuthProvider;
import com.deeprobotics.station.openapi.sdk.auth.AuthenticatedRequest;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiBusinessException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTransportException;
import com.deeprobotics.station.openapi.sdk.http.HttpConnectionException;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.Test;

/**
 * 资源流式下载 API 契约测试。
 *
 * <p>{@code /remoteApi/result/resource} 和普通 JSON 接口不同：成功响应是二进制流，失败响应可能是
 * JSON {@code R<?>}。这些测试锁定 SDK 对客户可见的下载语义：正常二进制不解包、JSON 失败转业务异常、
 * Path 下载用目标目录临时文件落盘，失败后不留下半截文件。</p>
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
    void pathDownloadWritesTargetAndCleansPartFileOnSuccess() throws IOException {
        Path directory = Files.createTempDirectory("station-openapi-result-download-");
        try {
            RecordingTransport transport = new RecordingTransport()
                    .enqueueDownloadBytes("path-file".getBytes(StandardCharsets.UTF_8));
            ResultApi api = new DefaultResultApi(newExecutor(transport, new PassthroughAuthProvider()));
            Path target = directory.resolve("patrol-result.jpg");

            Path downloaded = api.downloadResource(ResourceDownloadRequest.builder()
                    .filePath("result/TE-001/P001.jpg")
                    .type(ResourceDownloadRequest.Type.RESULT_RESOURCE)
                    .build(), target);

            assertThat(downloaded).isEqualTo(target);
            assertThat(Files.readString(target, StandardCharsets.UTF_8)).isEqualTo("path-file");
            assertThat(partFiles(directory)).isEmpty();
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    void pathDownloadDoesNotDeleteCustomerOwnedFileWithPartSuffix() throws IOException {
        Path directory = Files.createTempDirectory("station-openapi-result-download-");
        try {
            RecordingTransport transport = new RecordingTransport()
                    .enqueueDownloadBytes("new-download".getBytes(StandardCharsets.UTF_8));
            ResultApi api = new DefaultResultApi(newExecutor(transport, new PassthroughAuthProvider()));
            Path target = directory.resolve("patrol-result.jpg");
            Path customerOwnedPartFile = directory.resolve("patrol-result.jpg.part");
            Files.writeString(customerOwnedPartFile, "customer-owned-data", StandardCharsets.UTF_8);

            api.downloadResource(ResourceDownloadRequest.builder()
                    .filePath("result/TE-001/P001.jpg")
                    .type(ResourceDownloadRequest.Type.RESULT_RESOURCE)
                    .build(), target);

            assertThat(Files.readString(target, StandardCharsets.UTF_8)).isEqualTo("new-download");
            assertThat(customerOwnedPartFile).exists();
            assertThat(Files.readString(customerOwnedPartFile, StandardCharsets.UTF_8))
                    .isEqualTo("customer-owned-data");
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    void failedPathDownloadRemovesPartFileAndDoesNotCreateTarget() throws IOException {
        Path directory = Files.createTempDirectory("station-openapi-result-download-");
        try {
            RecordingTransport transport = new RecordingTransport()
                    .enqueueDownloadResponse(200, Map.of("Content-Type", List.of("application/json")),
                            failureBody(40004, "file not exists"));
            ResultApi api = new DefaultResultApi(newExecutor(transport, new PassthroughAuthProvider()));
            Path target = directory.resolve("missing.jpg");

            assertThatThrownBy(() -> api.downloadResource(ResourceDownloadRequest.builder()
                    .filePath("result/missing.jpg")
                    .type(2)
                    .build(), target))
                    .isInstanceOf(StationOpenApiBusinessException.class)
                    .hasMessageContaining("file not exists");

            assertThat(Files.exists(target)).isFalse();
            assertThat(partFiles(directory)).isEmpty();
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    void generatedRequestIdIsKeptWhenPathPreparationFailsLocally() throws IOException {
        Path directory = Files.createTempDirectory("station-openapi-result-download-");
        try {
            Path parentFile = directory.resolve("not-a-directory");
            Files.writeString(parentFile, "occupied", StandardCharsets.UTF_8);
            RecordingTransport transport = new RecordingTransport();
            ResultApi api = new DefaultResultApi(newExecutor(transport, new PassthroughAuthProvider()));
            ResourceDownloadRequest request = ResourceDownloadRequest.builder()
                    .filePath("result/file.bin")
                    .type(ResourceDownloadRequest.Type.RESULT_RESOURCE)
                    .build();

            assertThatThrownBy(() -> api.downloadResource(request, parentFile.resolve("target.bin")))
                    .isInstanceOfSatisfying(StationOpenApiDownloadException.class, failure -> {
                        assertThat(failure.requestId()).isNotBlank();
                        assertThat(failure.path()).isEqualTo("/remoteApi/result/resource");
                        assertThat(failure.operation()).isEqualTo("result.downloadResource");
                        assertThat(failure.resultUnknown()).isFalse();
                    });
            assertThat(transport.downloadRequests()).isEmpty();
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    void rootPathDownloadIsRejectedBeforeTransportCall() {
        RecordingTransport transport = new RecordingTransport();
        ResultApi api = new DefaultResultApi(newExecutor(transport, new PassthroughAuthProvider()));
        ResourceDownloadRequest request = ResourceDownloadRequest.builder()
                .filePath("result/file.bin")
                .type(ResourceDownloadRequest.Type.RESULT_RESOURCE)
                .build();
        Path root = Path.of("").toAbsolutePath().getRoot();

        assertThatThrownBy(() -> api.downloadResource(request, root))
                .isInstanceOf(StationOpenApiClientException.class)
                .hasMessageContaining("target must identify a file");
        assertThat(transport.downloadRequests()).isEmpty();
    }

    @Test
    void fileDownloadRetryReauthenticatesAndRebuildsSignedBody() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadFailure(new HttpConnectionException("connection reset"))
                .enqueueDownloadBytes("retry-file".getBytes(StandardCharsets.UTF_8));
        SequencedAuthProvider authProvider = new SequencedAuthProvider();
        ResultApi api = new DefaultResultApi(newExecutor(transport, authProvider));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        api.downloadResource(ResourceDownloadRequest.builder()
                .filePath("result/retry.jpg")
                .type(2)
                .build(), output);

        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("retry-file");
        assertThat(transport.downloadRequests()).hasSize(2);
        assertThat(authProvider.calls()).isEqualTo(2);
        Map<String, Object> firstBody = parseBody(transport.downloadRequests().get(0));
        Map<String, Object> retryBody = parseBody(transport.downloadRequests().get(1));
        assertThat(firstBody).containsEntry("nonce", "nonce-1").containsEntry("sign", "sign-1");
        assertThat(retryBody).containsEntry("nonce", "nonce-2").containsEntry("sign", "sign-2");
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

    private static List<Path> partFiles(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".part"))
                    .toList();
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
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

        private RecordingTransport enqueueDownloadFailure(RuntimeException failure) {
            downloadOutcomes.add(failure);
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
            if (outcome instanceof RuntimeException failure) {
                throw failure;
            }
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
