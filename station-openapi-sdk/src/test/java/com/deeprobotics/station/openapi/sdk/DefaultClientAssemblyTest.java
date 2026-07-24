package com.deeprobotics.station.openapi.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DogPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.DogInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * 从默认客户 artifact 验证公共 builder 到真实 HTTP 请求的完整装配链。
 *
 * <p>测试刻意不导入 SDK internal 类型，确保示例形态与普通客户可使用的公共 API 一致。</p>
 */
class DefaultClientAssemblyTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final MockWebServer server = new MockWebServer();

    @AfterEach
    void shutdownServer() throws IOException {
        server.shutdown();
    }

    @Test
    void defaultAggregateClientCallsTimeWithToken() throws Exception {
        server.enqueue(jsonResponse("""
                {"success":true,"code":200,"msg":"OK","data":"2026-07-10T20:30:00"}
                """));

        try (StationOpenApiClient client = StationOpenApiClients.builder()
                .endpoint(server.url("/web/").toString())
                .accessToken("customer-token")
                .build()) {
            assertThat(client.system().getTimeText()).isEqualTo("2026-07-10T20:30:00");
        }

        RecordedRequest request = takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/web/remoteApi/time");
        assertThat(request.getHeader("X-Access-Token")).isEqualTo("customer-token");
        assertThat(readBody(request)).isEmpty();
    }

    @Test
    void defaultAggregateClientCallsTimeWithSignatureWithoutSendingSecretKey() throws Exception {
        server.enqueue(jsonResponse("""
                {"success":true,"code":200,"msg":"OK","data":"2026-07-10T20:31:00"}
                """));

        try (StationOpenApiClient client = StationOpenApiClients.builder()
                .endpoint(server.url("/").toString())
                .appKey("customer-app")
                .secretKey("customer-secret")
                .build()) {
            assertThat(client.system().getTimeText()).isEqualTo("2026-07-10T20:31:00");
        }

        RecordedRequest request = takeRequest();
        Map<String, Object> body = readBody(request);
        assertThat(request.getPath()).isEqualTo("/remoteApi/time");
        assertThat(request.getHeader("X-Access-Token")).isNull();
        assertThat(body.keySet()).containsExactlyInAnyOrder("appKey", "timestamp", "nonce", "sign");
        assertThat(body).containsEntry("appKey", "customer-app").doesNotContainKey("secretKey");
        assertThat(request.getBody().readUtf8()).doesNotContain("customer-secret");
    }

    @Test
    void defaultAggregateClientSendsPageNoButNeverPageNum() throws Exception {
        server.enqueue(jsonResponse("""
                {"success":true,"code":200,"msg":"OK","data":{
                  "records":[],"total":0,"current":2,"size":15,"pages":0
                }}
                """));

        PageResult<DogInfo> result;
        try (StationOpenApiClient client = StationOpenApiClients.builder()
                .endpoint(server.url("/").toString())
                .accessToken("customer-token")
                .build()) {
            result = client.inventory().pageDogs(DogPageRequest.builder()
                    .pageNo(2)
                    .pageSize(15)
                    .dogName("Alpha")
                    .build());
        }

        RecordedRequest request = takeRequest();
        Map<String, Object> body = readBody(request);
        assertThat(result.records()).isEmpty();
        assertThat(result.pageNo()).isEqualTo(2L);
        assertThat(request.getPath()).isEqualTo("/remoteApi/dogPage");
        assertThat(body)
                .containsEntry("pageNo", 2)
                .containsEntry("pageSize", 15)
                .containsEntry("dogName", "Alpha")
                .doesNotContainKey("pageNum");
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(body);
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        return request;
    }

    private static Map<String, Object> readBody(RecordedRequest request) throws IOException {
        return JSON.readValue(request.getBody().clone().readByteArray(), new TypeReference<>() {
        });
    }
}
