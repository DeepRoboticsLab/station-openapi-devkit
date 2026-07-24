package com.deeprobotics.station.openapi.sdk.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiConfig;
import com.deeprobotics.station.openapi.sdk.auth.AuthenticatedRequest;
import com.deeprobotics.station.openapi.sdk.auth.AuthProvider;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiBusinessException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiDownloadException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiHttpException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTimeoutException;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTransportException;
import com.deeprobotics.station.openapi.sdk.http.EndpointDefinition;
import com.deeprobotics.station.openapi.sdk.http.HttpConnectionException;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTimeoutException;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.http.OperationIntent;
import com.deeprobotics.station.openapi.sdk.json.JsonCodec;
import com.deeprobotics.station.openapi.sdk.json.TypeRef;
import com.deeprobotics.station.openapi.sdk.model.MapUtils;
import com.deeprobotics.station.openapi.sdk.model.OpenApiRequest;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.Test;

/**
 * OpenApiExecutor 的核心执行链路测试。
 *
 * <p>本类覆盖 core 层负责的职责：默认 header、鉴权重建、JSON 响应解包、重试策略、
 * 下载临时文件隔离以及 SDK 异常上下文。transport 只返回底层 HTTP 结果，客户可见语义必须在这里统一收口。</p>
 */
class OpenApiExecutorTest {

    private final JsonCodec json = JsonCodec.defaultCodec();

    @Test
    void jsonRequestsSetDefaultHeadersAndRequestId() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueJson(200, ApiResponse.success("2026-07-08T10:20:30"));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        String data = executor.execute(timeEndpoint(), new StaticRequest(), new TypeRef<ApiResponse<String>>() {
        }, RequestOptions.builder().requestId("request-1").build());

        HttpRequest sent = transport.jsonRequests().get(0);
        assertThat(data).isEqualTo("2026-07-08T10:20:30");
        assertThat(sent.headers())
                .containsEntry("Content-Type", "application/json; charset=utf-8")
                .containsEntry("Accept", "application/json")
                .containsEntry("X-Request-Id", "request-1");
        assertThat(sent.headers().get("User-Agent")).startsWith("station-openapi-java/");
    }

    @Test
    void requiredDataRejectsSuccessfulNullWithGeneratedRequestContext() {
        RecordingTransport transport = new RecordingTransport().enqueueJson(200, ApiResponse.success(null));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        assertThatThrownBy(() -> executor.executeRequiredData(
                timeEndpoint(), new StaticRequest(), new TypeRef<ApiResponse<String>>() {
                }, null))
                .isInstanceOfSatisfying(StationOpenApiSerializationException.class, failure -> {
                    String requestId = transport.jsonRequests().get(0).headers().get("X-Request-Id");
                    assertThat(requestId).isNotBlank();
                    assertThat(failure.requestId()).isEqualTo(requestId);
                    assertThat(failure.path()).isEqualTo("/remoteApi/time");
                    assertThat(failure.operation()).isEqualTo("system.getTime");
                    assertThat(failure.resultUnknown()).isFalse();
                })
                .hasMessageContaining("response data is required");
    }

    @Test
    void nullableDataExecutionStillReturnsNull() {
        RecordingTransport transport = new RecordingTransport().enqueueJson(200, ApiResponse.success(null));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        String result = executor.execute(
                EndpointDefinition.of("task.issue", "/remoteApi/issue", OperationIntent.TASK_CONTROL),
                new StaticRequest("taskCode", "TASK-001"), new TypeRef<ApiResponse<String>>() {
                }, RequestOptions.builder().requestId("request-nullable-data").build());

        assertThat(result).isNull();
    }

    @Test
    void httpNon2xxThrowsHttpExceptionWithContext() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueJson(500, Map.of("success", false, "code", 50000, "msg", "server failed"));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        assertThatThrownBy(() -> executor.execute(timeEndpoint(), new StaticRequest(),
                new TypeRef<ApiResponse<String>>() {
                }, RequestOptions.builder().requestId("request-2").build()))
                .isInstanceOf(StationOpenApiHttpException.class)
                .hasMessageContaining("code=500")
                .hasMessageContaining("/remoteApi/time")
                .hasMessageContaining("system.getTime");
    }

    @Test
    void truncatedHttpErrorSnippetDoesNotExposeCompleteSensitiveValue() {
        String secret = "raw-access-token-at-snippet-boundary";
        String jsonPrefix = "{\"accessToken\":\"";
        String body = "x".repeat(4096 - jsonPrefix.length() - secret.length())
                + jsonPrefix + secret + "\"}";
        RecordingTransport transport = new RecordingTransport()
                .enqueueJsonBytes(500, body.getBytes(StandardCharsets.UTF_8));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        assertThatThrownBy(() -> executor.execute(timeEndpoint(), new StaticRequest(),
                new TypeRef<ApiResponse<String>>() {
                }, RequestOptions.builder().requestId("request-redaction-boundary").build()))
                .isInstanceOf(StationOpenApiHttpException.class)
                .hasMessageContaining("<redacted>")
                .hasMessageNotContaining(secret);
    }

    @Test
    void nullJsonEnvelopeThrowsSerializationExceptionWithEndpointContext() {
        RecordingTransport transport = new RecordingTransport().enqueueJson(200, null);
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        assertThatThrownBy(() -> executor.execute(timeEndpoint(), new StaticRequest(),
                new TypeRef<ApiResponse<String>>() {
                }, RequestOptions.builder().requestId("request-null-envelope").build()))
                .isInstanceOf(com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException.class)
                .hasMessageContaining("/remoteApi/time")
                .hasMessageContaining("system.getTime")
                .hasMessageContaining("request-null-envelope");
    }

    @Test
    void unsuccessfulEnvelopeThrowsBusinessException() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueJson(200, new ApiResponse<>(false, 40001, "business failed", null));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        assertThatThrownBy(() -> executor.execute(timeEndpoint(), new StaticRequest(),
                new TypeRef<ApiResponse<String>>() {
                }, RequestOptions.builder().requestId("request-3").build()))
                .isInstanceOf(StationOpenApiBusinessException.class)
                .hasMessageContaining("code=40001")
                .hasMessageContaining("business failed");
    }

    @Test
    void non200EnvelopeCodeThrowsBusinessExceptionEvenWhenSuccessIsTrue() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueJson(200, new ApiResponse<>(true, 50001, "backend code failed", null));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        assertThatThrownBy(() -> executor.execute(timeEndpoint(), new StaticRequest(),
                new TypeRef<ApiResponse<String>>() {
                }, RequestOptions.builder().requestId("request-4").build()))
                .isInstanceOf(StationOpenApiBusinessException.class)
                .hasMessageContaining("code=50001")
                .hasMessageContaining("backend code failed");
    }

    @Test
    void readOnlyRetriesOnceByDefault() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueJsonFailure(new HttpConnectionException("connection reset"))
                .enqueueJson(200, ApiResponse.success("ok"));
        SequencedAuthProvider authProvider = new SequencedAuthProvider();
        OpenApiExecutor executor = newExecutor(transport, authProvider);

        String data = executor.execute(timeEndpoint(), new StaticRequest(), new TypeRef<ApiResponse<String>>() {
        }, RequestOptions.builder().build());

        assertThat(data).isEqualTo("ok");
        assertThat(transport.jsonRequests()).hasSize(2);
        assertThat(authProvider.calls()).isEqualTo(2);
    }

    @Test
    void fileDownloadRetriesOnceByDefault() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadFailure(new HttpConnectionException("connection reset"))
                .enqueueDownload("file-bytes".getBytes(StandardCharsets.UTF_8));
        SequencedAuthProvider authProvider = new SequencedAuthProvider();
        OpenApiExecutor executor = newExecutor(transport, authProvider);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        executor.download(EndpointDefinition.of("result.downloadResource", "/remoteApi/result/resource",
                OperationIntent.FILE_DOWNLOAD), new StaticRequest("filePath", "map.bin"), output,
                RequestOptions.builder().build());

        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("file-bytes");
        assertThat(transport.downloadRequests()).hasSize(2);
        assertThat(authProvider.calls()).isEqualTo(2);
    }

    @Test
    void fileDownloadRetryDiscardsPartialBytesFromFailedAttempt() {
        // 第一次下载已经写入临时文件后失败，重试成功时客户 outputStream 只能看到第二次完整内容。
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadPartialThenFailure("partial-".getBytes(StandardCharsets.UTF_8),
                        new HttpConnectionException("connection reset"))
                .enqueueDownload("complete-bytes".getBytes(StandardCharsets.UTF_8));
        SequencedAuthProvider authProvider = new SequencedAuthProvider();
        OpenApiExecutor executor = newExecutor(transport, authProvider);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        executor.download(EndpointDefinition.of("result.downloadResource", "/remoteApi/result/resource",
                OperationIntent.FILE_DOWNLOAD), new StaticRequest("filePath", "map.bin"), output,
                RequestOptions.builder().build());

        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("complete-bytes");
        assertThat(transport.downloadRequests()).hasSize(2);
        assertThat(authProvider.calls()).isEqualTo(2);
    }

    @Test
    void fileDownloadRetryDeletesAttemptTempFiles() throws IOException {
        // 每次尝试都会生成独立临时文件；失败尝试和成功尝试结束后都应清理，避免长期运行进程堆积 .tmp 文件。
        Path tempDirectory = Files.createTempDirectory("station-openapi-executor-test-");
        try {
            RecordingTransport transport = new RecordingTransport()
                    .enqueueDownloadPartialThenFailure("partial-".getBytes(StandardCharsets.UTF_8),
                            new HttpConnectionException("connection reset"))
                    .enqueueDownload("complete-bytes".getBytes(StandardCharsets.UTF_8));
            SequencedAuthProvider authProvider = new SequencedAuthProvider();
            OpenApiExecutor executor = newExecutor(transport, authProvider, tempDirectory);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            executor.download(EndpointDefinition.of("result.downloadResource", "/remoteApi/result/resource",
                    OperationIntent.FILE_DOWNLOAD), new StaticRequest("filePath", "map.bin"), output,
                    RequestOptions.builder().build());

            assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("complete-bytes");
            assertThat(listDirectory(tempDirectory)).isEmpty();
        } finally {
            deleteDirectory(tempDirectory);
        }
    }

    @Test
    void downloadNon2xxResponseThrowsHttpExceptionWithEndpointContext() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadResponse(503, Map.of("success", false, "code", 50300, "msg", "download failed"));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        assertThatThrownBy(() -> executor.download(EndpointDefinition.of("result.downloadResource",
                "/remoteApi/result/resource", OperationIntent.FILE_DOWNLOAD),
                new StaticRequest("filePath", "map.bin"), new ByteArrayOutputStream(),
                RequestOptions.builder().requestId("request-download-1").build()))
                .isInstanceOf(StationOpenApiHttpException.class)
                .hasMessageContaining("code=503")
                .hasMessageContaining("/remoteApi/result/resource")
                .hasMessageContaining("result.downloadResource")
                .hasMessageContaining("request-download-1")
                .hasMessageContaining("download failed");
    }

    @Test
    void downloadJsonFailureResponseThrowsBusinessExceptionAndDoesNotWriteCallerOutput() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadResponse(200, Map.of("Content-Type", List.of("Application/Json; Charset=UTF-8")),
                        new ApiResponse<>(false, 40003, "download permission denied", null));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThatThrownBy(() -> executor.download(EndpointDefinition.of("result.downloadResource",
                "/remoteApi/result/resource", OperationIntent.FILE_DOWNLOAD),
                new StaticRequest("filePath", "map.bin"), output,
                RequestOptions.builder().requestId("request-download-2").build()))
                .isInstanceOf(StationOpenApiBusinessException.class)
                .hasMessageContaining("code=40003")
                .hasMessageContaining("/remoteApi/result/resource")
                .hasMessageContaining("result.downloadResource")
                .hasMessageContaining("request-download-2")
                .hasMessageContaining("download permission denied");
        assertThat(output.size()).isZero();
    }

    @Test
    void downloadJsonSuccessResponseThrowsDownloadExceptionAndDoesNotWriteCallerOutput() {
        // 下载接口协议要求成功响应是二进制流。即使 JSON envelope 标记 success，也不能当成文件写给客户。
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadResponse(200, Map.of("Content-Type", List.of("application/json")),
                        ApiResponse.success("not-a-binary-file"));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThatThrownBy(() -> executor.download(EndpointDefinition.of("result.downloadResource",
                "/remoteApi/result/resource", OperationIntent.FILE_DOWNLOAD),
                new StaticRequest("filePath", "map.bin"), output,
                RequestOptions.builder().requestId("request-download-json-success").build()))
                .isInstanceOf(StationOpenApiDownloadException.class)
                .hasMessageContaining("/remoteApi/result/resource")
                .hasMessageContaining("result.downloadResource")
                .hasMessageContaining("request-download-json-success")
                .satisfies(thrown -> assertThat(((StationOpenApiDownloadException) thrown).resultUnknown())
                        .isFalse());
        assertThat(output.size()).isZero();
    }

    @Test
    void downloadProblemJsonAndNullEnvelopeAreNeverWrittenAsFiles() {
        RecordingTransport problemTransport = new RecordingTransport()
                .enqueueDownloadResponse(200, Map.of("Content-Type", List.of("application/problem+json")),
                        new ApiResponse<>(false, 40003, "download denied", null));
        ByteArrayOutputStream problemOutput = new ByteArrayOutputStream();

        assertThatThrownBy(() -> newExecutor(problemTransport, new SequencedAuthProvider()).download(
                downloadEndpoint(), new StaticRequest("filePath", "map.bin"), problemOutput,
                RequestOptions.builder().requestId("request-problem-json").build()))
                .isInstanceOf(StationOpenApiBusinessException.class)
                .hasMessageContaining("download denied");
        assertThat(problemOutput.size()).isZero();

        RecordingTransport nullTransport = new RecordingTransport()
                .enqueueDownloadResponse(200, Map.of("Content-Type", List.of("application/json")), null);
        ByteArrayOutputStream nullOutput = new ByteArrayOutputStream();

        assertThatThrownBy(() -> newExecutor(nullTransport, new SequencedAuthProvider()).download(
                downloadEndpoint(), new StaticRequest("filePath", "map.bin"), nullOutput,
                RequestOptions.builder().requestId("request-null-download-envelope").build()))
                .isInstanceOf(com.deeprobotics.station.openapi.sdk.exception.StationOpenApiSerializationException.class)
                .hasMessageContaining("request-null-download-envelope");
        assertThat(nullOutput.size()).isZero();
    }

    @Test
    void downloadRejectsUnexpectedSuccessfulContentType() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadResponse(200, Map.of("Content-Type", List.of("text/html")),
                        "<html>gateway login</html>");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThatThrownBy(() -> newExecutor(transport, new SequencedAuthProvider()).download(
                downloadEndpoint(), new StaticRequest("filePath", "map.bin"), output,
                RequestOptions.builder().requestId("request-unexpected-mime").build()))
                .isInstanceOf(StationOpenApiDownloadException.class)
                .hasMessageContaining("Content-Type")
                .hasMessageContaining("text/html");
        assertThat(output.size()).isZero();
    }

    @Test
    void transportDownloadExceptionDuringTempWriteHasEndpointContextAndKnownResult() {
        // transport 失败发生在 SDK 临时文件写入阶段，客户 outputStream 尚未被写入，结果应明确为非未知。
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownloadPartialThenFailure("partial-temp".getBytes(StandardCharsets.UTF_8),
                        new StationOpenApiDownloadException("transport temp sink failed",
                                null, null, null, true));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThatThrownBy(() -> executor.download(EndpointDefinition.of("result.downloadResource",
                "/remoteApi/result/resource", OperationIntent.FILE_DOWNLOAD),
                new StaticRequest("filePath", "map.bin"), output,
                RequestOptions.builder().requestId("request-temp-write-failure").build()))
                .isInstanceOf(StationOpenApiDownloadException.class)
                .hasMessageContaining("/remoteApi/result/resource")
                .hasMessageContaining("result.downloadResource")
                .hasMessageContaining("request-temp-write-failure")
                .satisfies(thrown -> assertThat(((StationOpenApiDownloadException) thrown).resultUnknown())
                        .isFalse());
        assertThat(output.size()).isZero();
    }

    @Test
    void callerOutputFailureDuringFinalCopyHasResultUnknownTrue() {
        // 临时文件已完整拿到后，最终复制给客户流仍可能半途失败，此时客户侧文件是否完整无法判断。
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownload("complete-bytes".getBytes(StandardCharsets.UTF_8));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());
        FailingAfterPartialWriteOutputStream output = new FailingAfterPartialWriteOutputStream(4);

        assertThatThrownBy(() -> executor.download(EndpointDefinition.of("result.downloadResource",
                "/remoteApi/result/resource", OperationIntent.FILE_DOWNLOAD),
                new StaticRequest("filePath", "map.bin"), output,
                RequestOptions.builder().requestId("request-copy-failure").build()))
                .isInstanceOf(StationOpenApiDownloadException.class)
                .hasMessageContaining("request-copy-failure")
                .satisfies(thrown -> assertThat(((StationOpenApiDownloadException) thrown).resultUnknown())
                        .isTrue());
        assertThat(output.bytesWritten()).isPositive();
    }

    @Test
    void uncheckedCallerOutputFailureIsWrappedAndRedactedAsUnknownDownloadResult() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueDownload("complete-bytes".getBytes(StandardCharsets.UTF_8));
        RuntimeFailingOutputStream output = new RuntimeFailingOutputStream(4);

        assertThatThrownBy(() -> newExecutor(transport, new SequencedAuthProvider()).download(
                downloadEndpoint(), new StaticRequest("filePath", "map.bin"), output,
                RequestOptions.builder().requestId("request-runtime-output-failure").build()))
                .isInstanceOf(StationOpenApiDownloadException.class)
                .hasMessageContaining("request-runtime-output-failure")
                .hasMessageNotContaining("raw-camera-password")
                .satisfies(thrown -> assertThat(((StationOpenApiDownloadException) thrown).resultUnknown())
                        .isTrue());
        assertThat(output.bytesWritten()).isPositive();
    }

    @Test
    void tempCleanupFailureDoesNotMaskSuccessfulDownload() throws IOException {
        // 临时文件采用尽力清理策略，清理失败不能覆盖已经成功交付给客户的下载结果。
        Path tempDirectory = Files.createTempDirectory("station-openapi-executor-test-");
        try {
            RecordingTransport transport = new RecordingTransport()
                    .enqueueDownload("complete-bytes".getBytes(StandardCharsets.UTF_8));
            OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider(),
                    new FailingDeleteTempFileStore(tempDirectory));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            executor.download(EndpointDefinition.of("result.downloadResource", "/remoteApi/result/resource",
                    OperationIntent.FILE_DOWNLOAD), new StaticRequest("filePath", "map.bin"), output,
                    RequestOptions.builder().build());

            assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("complete-bytes");
        } finally {
            deleteDirectory(tempDirectory);
        }
    }

    @Test
    void tempCleanupRuntimeFailureAfterFailedAttemptDoesNotPreventRetry() throws IOException {
        // 清理失败不应中断后续重试，否则一次本地清理异常会放大为客户下载失败。
        Path tempDirectory = Files.createTempDirectory("station-openapi-executor-test-");
        try {
            RecordingTransport transport = new RecordingTransport()
                    .enqueueDownloadFailure(new HttpConnectionException("connection reset"))
                    .enqueueDownload("complete-bytes".getBytes(StandardCharsets.UTF_8));
            OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider(),
                    new RuntimeFailingDeleteTempFileStore(tempDirectory));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            executor.download(EndpointDefinition.of("result.downloadResource", "/remoteApi/result/resource",
                    OperationIntent.FILE_DOWNLOAD), new StaticRequest("filePath", "map.bin"), output,
                    RequestOptions.builder().build());

            assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("complete-bytes");
            assertThat(transport.downloadRequests()).hasSize(2);
        } finally {
            deleteDirectory(tempDirectory);
        }
    }

    @Test
    void tempCleanupRuntimeFailureDoesNotMaskDownloadHttpException() throws IOException {
        // 如果主失败是 HTTP 错误，finally 中的临时文件清理异常不能把调用方真正需要的平台错误覆盖掉。
        Path tempDirectory = Files.createTempDirectory("station-openapi-executor-test-");
        try {
            RecordingTransport transport = new RecordingTransport()
                    .enqueueDownloadResponse(503, Map.of("success", false, "code", 50300, "msg", "download failed"));
            OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider(),
                    new RuntimeFailingDeleteTempFileStore(tempDirectory));

            assertThatThrownBy(() -> executor.download(EndpointDefinition.of("result.downloadResource",
                    "/remoteApi/result/resource", OperationIntent.FILE_DOWNLOAD),
                    new StaticRequest("filePath", "map.bin"), new ByteArrayOutputStream(),
                    RequestOptions.builder().requestId("request-download-3").build()))
                    .isInstanceOf(StationOpenApiHttpException.class)
                    .hasMessageContaining("code=503")
                    .hasMessageContaining("request-download-3");
        } finally {
            deleteDirectory(tempDirectory);
        }
    }

    @Test
    void writeAndControlIntentsDoNotRetryByDefault() {
        assertNoDefaultRetry(OperationIntent.BUSINESS_WRITE);
        assertNoDefaultRetry(OperationIntent.TASK_CONTROL);
        assertNoDefaultRetry(OperationIntent.DEVICE_CONTROL);
    }

    @Test
    void requestOptionsNeverDisablesRetryForReadOnlyOperations() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueJsonFailure(new HttpConnectionException("connection reset"));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        assertThatThrownBy(() -> executor.execute(timeEndpoint(), new StaticRequest(),
                new TypeRef<ApiResponse<String>>() {
                }, RequestOptions.builder().retryPolicy(RequestOptions.RetryPolicy.NEVER).build()))
                .isInstanceOf(com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTransportException.class);

        assertThat(transport.jsonRequests()).hasSize(1);
    }

    @Test
    void retryRebuildsBodyAndReauthenticatesWithFreshAuthFields() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueJsonFailure(new HttpConnectionException("connection reset"))
                .enqueueJson(200, ApiResponse.success("ok"));
        SequencedAuthProvider authProvider = new SequencedAuthProvider();
        CountingRequest request = new CountingRequest();
        OpenApiExecutor executor = newExecutor(transport, authProvider);

        executor.execute(timeEndpoint(), request, new TypeRef<ApiResponse<String>>() {
        }, RequestOptions.builder().build());

        Map<String, Object> firstBody = parseBody(transport.jsonRequests().get(0));
        Map<String, Object> retryBody = parseBody(transport.jsonRequests().get(1));
        Map<String, String> firstHeaders = transport.jsonRequests().get(0).headers();
        Map<String, String> retryHeaders = transport.jsonRequests().get(1).headers();

        assertThat(request.toMapCalls()).isEqualTo(2);
        assertThat(authProvider.calls()).isEqualTo(2);
        assertThat(firstBody.get("nonce")).isEqualTo("nonce-1");
        assertThat(retryBody.get("nonce")).isEqualTo("nonce-2");
        assertThat(firstHeaders).containsEntry("X-Auth-Attempt", "1");
        assertThat(retryHeaders).containsEntry("X-Auth-Attempt", "2");
        assertThat(firstBody.containsKey("sign")).as("first request has sign field").isTrue();
        assertThat(retryBody.containsKey("sign")).as("retry request has sign field").isTrue();
        assertThat(signChanged(firstBody, retryBody)).as("retry regenerates sign").isTrue();
    }

    @Test
    void timeoutOnWriteOrControlOperationHasResultUnknownTrue() {
        for (OperationIntent intent : List.of(OperationIntent.BUSINESS_WRITE, OperationIntent.TASK_CONTROL,
                OperationIntent.DEVICE_CONTROL)) {
            RecordingTransport transport = new RecordingTransport()
                    .enqueueJsonFailure(new HttpTimeoutException("read timeout"));
            OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

            assertThatThrownBy(() -> executor.execute(
                    EndpointDefinition.of("dangerous.operation", "/remoteApi/controlDog", intent),
                    new StaticRequest(), new TypeRef<ApiResponse<Void>>() {
                    }, RequestOptions.builder().build()))
                    .isInstanceOf(StationOpenApiTimeoutException.class)
                    .hasMessageContaining("Query status or wait for MQ confirmation before retrying")
                    .satisfies(thrown -> assertThat(
                            ((StationOpenApiTimeoutException) thrown).resultUnknown()).isTrue());
        }
    }

    @Test
    void connectionLossOnWriteOrControlOperationHasResultUnknownTrueAndDoesNotRetry() {
        for (OperationIntent intent : List.of(OperationIntent.BUSINESS_WRITE, OperationIntent.TASK_CONTROL,
                OperationIntent.DEVICE_CONTROL)) {
            RecordingTransport transport = new RecordingTransport()
                    .enqueueJsonFailure(new HttpConnectionException("connection reset"));
            OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

            assertThatThrownBy(() -> executor.execute(
                    EndpointDefinition.of("dangerous.operation", "/remoteApi/controlDog", intent),
                    new StaticRequest(), new TypeRef<ApiResponse<Void>>() {
                    }, RequestOptions.builder().build()))
                    .isInstanceOf(StationOpenApiTransportException.class)
                    .hasMessageContaining("Query status or wait for MQ confirmation before retrying")
                    .satisfies(thrown -> assertThat(
                            ((StationOpenApiTransportException) thrown).resultUnknown()).isTrue());
            assertThat(transport.jsonRequests()).hasSize(1);
        }
    }

    @Test
    void unknownTransportRuntimeFailureIsWrappedRedactedAndNeverRetried() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueJsonFailure(new IllegalStateException("transport token=raw-transport-token"));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        assertThatThrownBy(() -> executor.execute(
                EndpointDefinition.of("dangerous.operation", "/remoteApi/controlDog", OperationIntent.DEVICE_CONTROL),
                new StaticRequest(), new TypeRef<ApiResponse<Void>>() {
                }, RequestOptions.builder().requestId("request-unknown-transport").build()))
                .isInstanceOf(StationOpenApiTransportException.class)
                .hasMessageContaining("request-unknown-transport")
                .hasMessageNotContaining("raw-transport-token")
                .satisfies(thrown -> assertThat(((StationOpenApiTransportException) thrown).resultUnknown())
                        .isTrue());
        assertThat(transport.jsonRequests()).hasSize(1);
    }

    @Test
    void nullTransportResponsesAreWrappedWithSdkContext() {
        HttpTransport transport = new HttpTransport() {
            @Override
            public HttpResponse postJson(HttpRequest request) {
                return null;
            }

            @Override
            public HttpResponse download(HttpRequest request, OutputStream outputStream) {
                return null;
            }

            @Override
            public void close() {
            }
        };
        StationOpenApiConfig config = StationOpenApiConfig.builder()
                .endpoint("https://station.example.com")
                .accessToken("token-value")
                .build();
        OpenApiExecutor executor = new OpenApiExecutor(
                config, new SequencedAuthProvider(), transport, json);

        assertThatThrownBy(() -> executor.execute(
                timeEndpoint(), new StaticRequest(), new TypeRef<ApiResponse<String>>() {
                }, RequestOptions.builder().requestId("request-null-json").build()))
                .isInstanceOfSatisfying(StationOpenApiTransportException.class, failure -> {
                    assertThat(failure.path()).isEqualTo("/remoteApi/time");
                    assertThat(failure.operation()).isEqualTo("system.getTime");
                    assertThat(failure.requestId()).isEqualTo("request-null-json");
                    assertThat(failure.resultUnknown()).isFalse();
                })
                .rootCause()
                .hasMessageContaining("HttpTransport.postJson returned null");

        assertThatThrownBy(() -> executor.download(
                downloadEndpoint(), new StaticRequest("filePath", "map.bin"), new ByteArrayOutputStream(),
                RequestOptions.builder().requestId("request-null-download").build()))
                .isInstanceOfSatisfying(StationOpenApiTransportException.class, failure -> {
                    assertThat(failure.path()).isEqualTo("/remoteApi/result/resource");
                    assertThat(failure.operation()).isEqualTo("result.downloadResource");
                    assertThat(failure.requestId()).isEqualTo("request-null-download");
                    assertThat(failure.resultUnknown()).isFalse();
                })
                .rootCause()
                .hasMessageContaining("HttpTransport.download returned null");
    }

    private void assertNoDefaultRetry(OperationIntent intent) {
        RecordingTransport transport = new RecordingTransport()
                .enqueueJsonFailure(new HttpConnectionException("connection reset"));
        OpenApiExecutor executor = newExecutor(transport, new SequencedAuthProvider());

        assertThatThrownBy(() -> executor.execute(
                EndpointDefinition.of("dangerous.operation", "/remoteApi/controlDog", intent),
                new StaticRequest(), new TypeRef<ApiResponse<Void>>() {
                }, RequestOptions.builder().build()))
                .isInstanceOf(com.deeprobotics.station.openapi.sdk.exception.StationOpenApiTransportException.class);

        assertThat(transport.jsonRequests()).hasSize(1);
    }

    private static EndpointDefinition downloadEndpoint() {
        return EndpointDefinition.of("result.downloadResource", "/remoteApi/result/resource",
                OperationIntent.FILE_DOWNLOAD);
    }

    private OpenApiExecutor newExecutor(RecordingTransport transport, AuthProvider authProvider) {
        return newExecutor(transport, authProvider, (Path) null);
    }

    private OpenApiExecutor newExecutor(RecordingTransport transport, AuthProvider authProvider, Path tempDirectory) {
        StationOpenApiConfig config = StationOpenApiConfig.builder()
                .endpoint("http://host:23288")
                .accessToken("token-value")
                .build();
        if (tempDirectory == null) {
            return new OpenApiExecutor(config, authProvider, transport, json);
        }
        return new OpenApiExecutor(config, authProvider, transport, json, tempDirectory);
    }

    private OpenApiExecutor newExecutor(RecordingTransport transport, AuthProvider authProvider,
            OpenApiExecutor.DownloadTempFileStore tempFileStore) {
        StationOpenApiConfig config = StationOpenApiConfig.builder()
                .endpoint("http://host:23288")
                .accessToken("token-value")
                .build();
        return new OpenApiExecutor(config, authProvider, transport, json, tempFileStore);
    }

    private static EndpointDefinition timeEndpoint() {
        return EndpointDefinition.of("system.getTime", "/remoteApi/time", OperationIntent.READ_ONLY);
    }

    private Map<String, Object> parseBody(HttpRequest request) {
        return json.read(request.body(), new TypeRef<LinkedHashMap<String, Object>>() {
        });
    }

    private static boolean signChanged(Map<String, Object> firstBody, Map<String, Object> retryBody) {
        Object firstSign = firstBody.get("sign");
        Object retrySign = retryBody.get("sign");
        return firstSign instanceof String
                && retrySign instanceof String
                && !firstSign.equals(retrySign);
    }

    private static List<Path> listDirectory(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream.toList();
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        for (Path child : listDirectory(directory)) {
            Files.deleteIfExists(child);
        }
        Files.deleteIfExists(directory);
    }

    private static final class StaticRequest implements OpenApiRequest {
        private final LinkedHashMap<String, Object> body = MapUtils.newBody();

        private StaticRequest(Object... keyValues) {
            for (int i = 0; i < keyValues.length; i += 2) {
                body.put((String) keyValues[i], keyValues[i + 1]);
            }
        }

        @Override
        public LinkedHashMap<String, Object> toMap() {
            return MapUtils.copyWithoutNulls(body);
        }
    }

    private static final class CountingRequest implements OpenApiRequest {
        private int toMapCalls;

        @Override
        public LinkedHashMap<String, Object> toMap() {
            toMapCalls++;
            LinkedHashMap<String, Object> body = MapUtils.newBody();
            body.put("dogCode", "DOG-001");
            return body;
        }

        private int toMapCalls() {
            return toMapCalls;
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

    private final class RecordingTransport implements HttpTransport {
        // 队列按调用顺序返回预设结果，用于精确模拟“失败后重试”“部分写入后失败”等链路。
        private final Queue<Object> jsonOutcomes = new ArrayDeque<>();
        private final Queue<Object> downloadOutcomes = new ArrayDeque<>();
        private final List<HttpRequest> jsonRequests = new ArrayList<>();
        private final List<HttpRequest> downloadRequests = new ArrayList<>();

        private RecordingTransport enqueueJson(int statusCode, Object responseBody) {
            jsonOutcomes.add(new HttpResponse(statusCode, Map.of(), json.writeBytes(wireBody(responseBody))));
            return this;
        }

        private RecordingTransport enqueueJsonBytes(int statusCode, byte[] responseBody) {
            jsonOutcomes.add(new HttpResponse(statusCode, Map.of(), responseBody));
            return this;
        }

        private RecordingTransport enqueueJsonFailure(RuntimeException failure) {
            jsonOutcomes.add(failure);
            return this;
        }

        private RecordingTransport enqueueDownload(byte[] bytes) {
            downloadOutcomes.add(bytes);
            return this;
        }

        private RecordingTransport enqueueDownloadPartialThenFailure(byte[] bytes, RuntimeException failure) {
            downloadOutcomes.add(new PartialDownloadFailure(bytes, failure));
            return this;
        }

        private RecordingTransport enqueueDownloadFailure(RuntimeException failure) {
            downloadOutcomes.add(failure);
            return this;
        }

        private RecordingTransport enqueueDownloadResponse(int statusCode, Object responseBody) {
            downloadOutcomes.add(new HttpResponse(statusCode, Map.of(), json.writeBytes(wireBody(responseBody))));
            return this;
        }

        private RecordingTransport enqueueDownloadResponse(int statusCode, Map<String, List<String>> headers,
                Object responseBody) {
            downloadOutcomes.add(new HttpResponse(statusCode, headers, json.writeBytes(wireBody(responseBody))));
            return this;
        }

        @Override
        public HttpResponse postJson(HttpRequest request) {
            jsonRequests.add(request);
            Object outcome = jsonOutcomes.remove();
            if (outcome instanceof RuntimeException failure) {
                throw failure;
            }
            return (HttpResponse) outcome;
        }

        @Override
        public HttpResponse download(HttpRequest request, OutputStream outputStream) {
            downloadRequests.add(request);
            Object outcome = downloadOutcomes.remove();
            if (outcome instanceof RuntimeException failure) {
                throw failure;
            }
            if (outcome instanceof PartialDownloadFailure partialDownloadFailure) {
                try {
                    // 先写入一段内容再抛异常，模拟真实下载中断时临时文件已有部分字节的场景。
                    outputStream.write(partialDownloadFailure.bytes());
                } catch (java.io.IOException ex) {
                    throw new AssertionError(ex);
                }
                throw partialDownloadFailure.failure();
            }
            if (outcome instanceof HttpResponse response) {
                return response;
            }
            try {
                outputStream.write((byte[]) outcome);
            } catch (java.io.IOException ex) {
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

        private Object wireBody(Object responseBody) {
            if (!(responseBody instanceof ApiResponse<?> apiResponse)) {
                return responseBody;
            }
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("success", apiResponse.success());
            body.put("code", apiResponse.code());
            body.put("msg", apiResponse.msg());
            body.put("data", apiResponse.data());
            return body;
        }
    }

    private record PartialDownloadFailure(byte[] bytes, RuntimeException failure) {
    }

    private static final class FailingAfterPartialWriteOutputStream extends OutputStream {
        private final int failAfterBytes;
        private int bytesWritten;

        private FailingAfterPartialWriteOutputStream(int failAfterBytes) {
            this.failAfterBytes = failAfterBytes;
        }

        @Override
        public void write(int value) throws IOException {
            // 用单字节写失败模拟最终复制阶段客户流半途不可写，确保 resultUnknown=true。
            if (bytesWritten >= failAfterBytes) {
                throw new IOException("caller output failed");
            }
            bytesWritten++;
        }

        private int bytesWritten() {
            return bytesWritten;
        }
    }

    private static final class RuntimeFailingOutputStream extends OutputStream {
        private final int failAfterBytes;
        private int bytesWritten;

        private RuntimeFailingOutputStream(int failAfterBytes) {
            this.failAfterBytes = failAfterBytes;
        }

        @Override
        public void write(int value) {
            if (bytesWritten >= failAfterBytes) {
                throw new IllegalStateException("caller stream password=raw-camera-password");
            }
            bytesWritten++;
        }

        private int bytesWritten() {
            return bytesWritten;
        }
    }

    private static final class FailingDeleteTempFileStore implements OpenApiExecutor.DownloadTempFileStore {
        private final Path directory;

        private FailingDeleteTempFileStore(Path directory) {
            this.directory = directory;
        }

        @Override
        public Path create() throws IOException {
            return Files.createTempFile(directory, "station-openapi-download-", ".tmp");
        }

        @Override
        public void delete(Path path) throws IOException {
            // 模拟 Windows 文件占用、权限不足或杀毒软件锁定导致的清理失败。
            throw new IOException("cleanup failed");
        }
    }

    private static final class RuntimeFailingDeleteTempFileStore implements OpenApiExecutor.DownloadTempFileStore {
        private final Path directory;

        private RuntimeFailingDeleteTempFileStore(Path directory) {
            this.directory = directory;
        }

        @Override
        public Path create() throws IOException {
            return Files.createTempFile(directory, "station-openapi-download-", ".tmp");
        }

        @Override
        public void delete(Path path) {
            // delete 也可能抛 RuntimeException，executor 的 finally 分支必须一并吞掉清理失败。
            throw new IllegalStateException("cleanup failed");
        }
    }
}
