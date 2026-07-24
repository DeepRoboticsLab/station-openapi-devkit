package com.deeprobotics.station.openapi.sdk.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deeprobotics.station.openapi.sdk.exception.StationOpenApiBusinessException;
import com.deeprobotics.station.openapi.sdk.model.common.ApiResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * SDK 统一 JSON 配置和 {@code R<T>} 基础语义测试。
 *
 * <p>锁定未知字段兼容、平台日期时间格式、统一响应字段解析，以及只有
 * {@code success=true && code=200} 才能解包成功的双条件规则。</p>
 */
class JsonCodecTest {

    private final JsonCodec json = JsonCodec.defaultCodec();

    @Test
    void jacksonCodecIgnoresUnknownResponseFields() {
        String body = "{\"success\":true,\"code\":200,\"msg\":\"OK\",\"data\":{\"name\":\"DOG-001\","
                + "\"unknown\":\"ignored\"},\"traceId\":\"noise\"}";

        ApiResponse<DogSummary> response = json.read(body, new TypeRef<ApiResponse<DogSummary>>() {
        });

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.msg()).isEqualTo("OK");
        assertThat(response.data().name()).isEqualTo("DOG-001");
    }

    @Test
    void javaTimeRequestFormattingUsesBackendWireFormats() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("startTime", LocalDateTime.of(2026, 7, 8, 9, 10, 11));
        body.put("dailyStartTime", LocalTime.of(8, 5, 6));

        String encoded = json.write(body);

        assertThat(encoded).contains("\"startTime\":\"2026-07-08 09:10:11\"");
        assertThat(encoded).contains("\"dailyStartTime\":\"08:05:06\"");
    }

    @Test
    void apiResponseParsesEnvelopeFieldsAndData() {
        String body = "{\"success\":true,\"code\":200,\"msg\":\"OK\",\"data\":\"server-time\"}";

        ApiResponse<String> response = json.read(body, new TypeRef<ApiResponse<String>>() {
        });

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.msg()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("server-time");
    }

    @Test
    void apiResponseSuccessRequiresSuccessTrueAndCode200() {
        assertThat(ApiResponse.success("ok").isSuccessful()).isTrue();
        assertThat(new ApiResponse<>(true, 201, "created", "ok").isSuccessful()).isFalse();
        assertThat(new ApiResponse<>(false, 200, "not ok", "ok").isSuccessful()).isFalse();
    }

    @Test
    void rFailureOrNon200CodeMapsToBusinessException() {
        ApiResponse<String> failure = new ApiResponse<>(false, 40001, "token=raw-token failed", null);
        ApiResponse<String> non200 = new ApiResponse<>(true, 50001, "backend failed", null);

        assertThatThrownBy(() -> failure.unwrapOrThrow("/remoteApi/taskPage", "request-1", "task.page"))
                .isInstanceOf(StationOpenApiBusinessException.class)
                .hasMessageContaining("code=40001")
                .hasMessageContaining("<redacted>")
                .hasMessageNotContaining("raw-token");
        assertThatThrownBy(() -> non200.unwrapOrThrow("/remoteApi/taskPage", "request-2", "task.page"))
                .isInstanceOf(StationOpenApiBusinessException.class)
                .hasMessageContaining("code=50001");
    }

    static final class DogSummary {
        private String name;

        public String name() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
