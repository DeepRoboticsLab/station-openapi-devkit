package com.deeprobotics.station.openapi.sdk.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClients;
import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiClientException;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.deeprobotics.station.openapi.sdk.model.request.result.ResourceDownloadRequest;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 客户业务 API 的公共 null 边界测试。
 *
 * <p>所有业务请求、下载目标和输出流都必须在进入 transport 前转换为统一 SDK 客户异常，
 * 避免客户为同一种本地输入错误额外捕获 JDK NPE。</p>
 */
class PublicApiInputValidationTest {

    @Test
    void everyPublicBusinessInputRejectsNullWithSdkClientExceptionBeforeIo() {
        NoCallTransport transport = new NoCallTransport();
        ResourceDownloadRequest downloadRequest = ResourceDownloadRequest.builder()
                .filePath("result/file.bin")
                .type(ResourceDownloadRequest.Type.RESULT_RESOURCE)
                .build();

        try (StationOpenApiClient client = StationOpenApiClients.builder()
                .endpoint("https://station.example.com")
                .accessToken("token-value")
                .transport(transport)
                .build()) {
            List<InvalidCall> invalidCalls = List.of(
                    new InvalidCall("inventory.pageDogs", () -> client.inventory().pageDogs(null, null)),
                    new InvalidCall("inventory.listMaps", () -> client.inventory().listMaps(null, null)),
                    new InvalidCall("inventory.pageStreams", () -> client.inventory().pageStreams(null, null)),
                    new InvalidCall("inventory.pagePoints", () -> client.inventory().pagePoints(null, null)),
                    new InvalidCall("inventory.pageMaintainAreas",
                            () -> client.inventory().pageMaintainAreas(null, null)),
                    new InvalidCall("inventory.listRoadMaps", () -> client.inventory().listRoadMaps(null, null)),
                    new InvalidCall("inventory.listDocks", () -> client.inventory().listDocks(null, null)),
                    new InvalidCall("inventory.listNodes", () -> client.inventory().listNodes(null, null)),
                    new InvalidCall("dog.getState", () -> client.dog().getState(null, null)),
                    new InvalidCall("dog.chargeControl", () -> client.dog().chargeControl(null, null)),
                    new InvalidCall("dog.control", () -> client.dog().control(null, null)),
                    new InvalidCall("dog.setPosition", () -> client.dog().setPosition(null, null)),
                    new InvalidCall("dog.pageAlarms", () -> client.dog().pageAlarms(null, null)),
                    new InvalidCall("dog.getPosition", () -> client.dog().getPosition(null, null)),
                    new InvalidCall("camera.control", () -> client.camera().control(null, null)),
                    new InvalidCall("camera.resetPtz", () -> client.camera().resetPtz(null, null)),
                    new InvalidCall("taskTemplate.create", () -> client.taskTemplate().create(null, null)),
                    new InvalidCall("taskTemplate.deleteByCodes",
                            () -> client.taskTemplate().deleteByCodes(null, null)),
                    new InvalidCall("taskTemplate.getByCode", () -> client.taskTemplate().getByCode(null, null)),
                    new InvalidCall("taskTemplate.page", () -> client.taskTemplate().page(null, null)),
                    new InvalidCall("task.issue", () -> client.task().issue(null, null)),
                    new InvalidCall("task.control", () -> client.task().control(null, null)),
                    new InvalidCall("task.batchCancel", () -> client.task().batchCancel(null, null)),
                    new InvalidCall("task.page", () -> client.task().page(null, null)),
                    new InvalidCall("result.pageTaskPointResults",
                            () -> client.result().pageTaskPointResults(null, null)),
                    new InvalidCall("result.downloadResource.request.outputStream",
                            () -> client.result().downloadResource(null, new ByteArrayOutputStream(), null)),
                    new InvalidCall("result.downloadResource.outputStream",
                            () -> client.result().downloadResource(downloadRequest, (OutputStream) null, null)),
                    new InvalidCall("result.downloadResource.request.path",
                            () -> client.result().downloadResource(null, Path.of("result.bin"), null)),
                    new InvalidCall("result.downloadResource.path",
                            () -> client.result().downloadResource(downloadRequest, (Path) null, null)));

            for (InvalidCall invalidCall : invalidCalls) {
                assertThatThrownBy(invalidCall.action()::run)
                        .as(invalidCall.name())
                        .isExactlyInstanceOf(StationOpenApiClientException.class)
                        .hasMessageContaining("required")
                        .satisfies(failure -> assertThat(
                                ((StationOpenApiClientException) failure).resultUnknown()).isFalse());
            }
        }

        assertThat(transport.calls).hasValue(0);
    }

    private record InvalidCall(String name, Runnable action) {
    }

    private static final class NoCallTransport implements HttpTransport {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public HttpResponse postJson(HttpRequest request) {
            calls.incrementAndGet();
            throw new AssertionError("invalid input must fail before postJson");
        }

        @Override
        public HttpResponse download(HttpRequest request, OutputStream outputStream) {
            calls.incrementAndGet();
            throw new AssertionError("invalid input must fail before download");
        }

        @Override
        public void close() {
        }
    }
}
