package com.deeprobotics.station.openapi.sample.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deeprobotics.station.openapi.sample.config.SampleSafetyProperties;
import com.deeprobotics.station.openapi.sample.safety.DangerousOperationInterceptor;
import com.deeprobotics.station.openapi.sample.service.TaskTemplateSampleService;
import com.deeprobotics.station.openapi.sample.web.advice.SampleExceptionHandler;
import com.deeprobotics.station.openapi.sample.web.i18n.SampleLocaleResolver;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClients;
import com.deeprobotics.station.openapi.sdk.http.HttpRequest;
import com.deeprobotics.station.openapi.sdk.http.HttpResponse;
import com.deeprobotics.station.openapi.sdk.http.HttpTimeoutException;
import com.deeprobotics.station.openapi.sdk.http.HttpTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 使用真实 Controller、Service 和 SDK，仅替换 HTTP 传输边界；不连接平台或设备。 */
class TaskTemplateStatusControllerTest {

    private static final String PATH = "/sample/api/task-template/switch-status";
    private RecordingTransport transport;
    private StationOpenApiClient client;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transport = new RecordingTransport();
        client = StationOpenApiClients.builder().endpoint("https://station.example.com/context")
                .accessToken("test-token").transport(transport).build();
        SampleSafetyProperties safety = new SampleSafetyProperties();
        safety.setDangerousOperationsEnabled(true);
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TaskTemplateSampleController(new TaskTemplateSampleService(client)))
                .setControllerAdvice(new SampleExceptionHandler(messages))
                .setLocaleResolver(new SampleLocaleResolver())
                .addInterceptors(new DangerousOperationInterceptor(safety))
                .build();
    }

    @AfterEach
    void closeClient() {
        client.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void enabledOperationSendsIntegerStateAndRequestIdThroughSdk(int state) throws Exception {
        mockMvc.perform(post(PATH).header("X-Request-Id", "sample-status-001")
                        .contentType(APPLICATION_JSON)
                        .content("{\"taskCode\":\" TM-001 \",\"isEnable\":" + state + "}"))
                .andExpect(status().isNoContent());

        assertThat(transport.requests).hasSize(1);
        HttpRequest request = transport.requests.get(0);
        assertThat(request.url().getPath()).isEqualTo("/context/remoteApi/switchTaskModelStatus");
        assertThat(request.headers()).containsEntry("X-Request-Id", "sample-status-001");
        var body = new ObjectMapper().readTree(request.body());
        assertThat(body.size()).isEqualTo(2);
        assertThat(body.path("taskCode").asText()).isEqualTo("TM-001");
        assertThat(body.path("isEnable").isIntegralNumber()).isTrue();
        assertThat(body.path("isEnable").asInt()).isEqualTo(state);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"taskCode\":\"TM-001\"}",
            "{\"taskCode\":\"TM-001\",\"isEnable\":null}",
            "{\"taskCode\":\"TM-001\",\"isEnable\":-1}",
            "{\"taskCode\":\"TM-001\",\"isEnable\":2}"})
    void missingOrInvalidStateFailsBeforeSdkIo(String body) throws Exception {
        mockMvc.perform(post(PATH).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SAMPLE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.field").value("isEnable"));
        assertThat(transport.requests).isEmpty();
    }

    @Test
    void blankTemplateCodeFailsBeforeSdkIo() throws Exception {
        mockMvc.perform(post(PATH).contentType(APPLICATION_JSON)
                        .content("{\"taskCode\":\" \",\"isEnable\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("taskCode"));
        assertThat(transport.requests).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "1.9", "\"1\"", "true", "2147483648"})
    void nonIntegerJsonStatesCannotBecomeAnEnableOrDisableCommand(String state) throws Exception {
        mockMvc.perform(post(PATH).contentType(APPLICATION_JSON)
                        .content("{\"taskCode\":\"TM-001\",\"isEnable\":" + state + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SAMPLE_VALIDATION_FAILED"));
        assertThat(transport.requests).isEmpty();
    }

    @Test
    void stateValidationMessagesFollowRequestedLanguage() throws Exception {
        for (String language : List.of("zh_CN", "en_US")) {
            mockMvc.perform(post(PATH + "?lang=" + language).contentType(APPLICATION_JSON)
                            .content("{\"taskCode\":\"TM-001\",\"isEnable\":2}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("zh_CN".equals(language)
                            ? "启用状态 isEnable 只能为 0（停用）或 1（启用）"
                            : "isEnable must be 0 (disabled) or 1 (enabled)."));
        }
        assertThat(transport.requests).isEmpty();
    }

    @Test
    void unreadableStateReturnsLocalizedProblemWithoutEchoingInput() throws Exception {
        for (String language : List.of("zh_CN", "en_US")) {
            String response = mockMvc.perform(post(PATH + "?lang=" + language).contentType(APPLICATION_JSON)
                            .content("{\"taskCode\":\"TM-001\",\"isEnable\":\"rejected-input-marker\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SAMPLE_VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.detail").value("zh_CN".equals(language)
                            ? "参数校验失败" : "Validation failed"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain("rejected-input-marker");
        }
        assertThat(transport.requests).isEmpty();
    }

    @Test
    void timeoutPreservesUnknownResultAndDoesNotRepeatTheWrite() throws Exception {
        transport.timeout = true;
        mockMvc.perform(post(PATH).contentType(APPLICATION_JSON)
                        .content("{\"taskCode\":\"TM-001\",\"isEnable\":0}"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.resultUnknown").value(true));
        assertThat(transport.requests).hasSize(1);
    }

    private static final class RecordingTransport implements HttpTransport {
        private final List<HttpRequest> requests = new ArrayList<>();
        private boolean timeout;

        @Override
        public HttpResponse postJson(HttpRequest request) {
            requests.add(request);
            if (timeout) {
                throw new HttpTimeoutException("test timeout");
            }
            return new HttpResponse(200, Map.of(),
                    "{\"code\":200,\"success\":true,\"data\":null}".getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public HttpResponse download(HttpRequest request, OutputStream outputStream) {
            throw new AssertionError("启停操作不能进入下载传输");
        }

        @Override
        public void close() {
        }
    }
}
