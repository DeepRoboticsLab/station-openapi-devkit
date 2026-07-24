package com.deeprobotics.station.openapi.sample.web;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deeprobotics.station.openapi.sample.service.ResultSampleService;
import com.deeprobotics.station.openapi.sample.service.SystemSampleService;
import com.deeprobotics.station.openapi.sample.web.advice.SampleExceptionHandler;
import com.deeprobotics.station.openapi.sample.web.dto.ResultInputs;
import com.deeprobotics.station.openapi.sample.web.i18n.SampleLocaleResolver;
import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.api.ResultApi;
import com.deeprobotics.station.openapi.sdk.api.SystemApi;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.result.ResourceDownloadRequest;
import com.deeprobotics.station.openapi.sdk.model.request.result.TaskPointResultPageRequest;
import java.io.OutputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ResultAndSystemControllerTest {

    private ResultApi resultApi;
    private SystemApi systemApi;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StationOpenApiClient client = mock(StationOpenApiClient.class);
        resultApi = mock(ResultApi.class);
        systemApi = mock(SystemApi.class);
        when(client.result()).thenReturn(resultApi);
        when(client.system()).thenReturn(systemApi);

        ResultSampleController resultController = new ResultSampleController(new ResultSampleService(client));
        SystemSampleController systemController = new SystemSampleController(new SystemSampleService(client));
        mockMvc = MockMvcBuilders.standaloneSetup(resultController, systemController)
                .setControllerAdvice(new SampleExceptionHandler(messageSource()))
                .setLocaleResolver(new SampleLocaleResolver())
                .build();
    }

    @Test
    void taskPointPageMapsPageNoAndReturnsSerializablePage() throws Exception {
        assertThat(new ResultInputs.PageTaskPointResults("E-1", 2, 30).toSdkRequest().toMap())
                .containsEntry("taskExecuteCode", "E-1")
                .containsEntry("pageNo", 2)
                .containsEntry("pageSize", 30)
                .doesNotContainKey("pageNum");
        when(resultApi.pageTaskPointResults(any(TaskPointResultPageRequest.class), any(RequestOptions.class)))
                .thenReturn(new PageResult<>(List.of(), 0L, 2L, 30L, 0L));

        mockMvc.perform(post("/sample/api/result/page-task-point-results")
                        .header("X-Request-Id", "request-result-page")
                        .contentType(APPLICATION_JSON)
                        .content("{\"taskExecuteCode\":\"E-1\",\"pageNo\":2,\"pageSize\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.pageNo").value(2));

        verify(resultApi).pageTaskPointResults(
                any(TaskPointResultPageRequest.class), any(RequestOptions.class));
    }

    @Test
    void resourceTypeOutsideOneAndTwoReturnsLocalizedProblemDetail() throws Exception {
        mockMvc.perform(post("/sample/api/result/download-resource")
                        .queryParam("lang", "en_US")
                        .contentType(APPLICATION_JSON)
                        .content("{\"filePath\":\"result/a.jpg\",\"type\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SAMPLE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.field").value("type"))
                .andExpect(jsonPath("$.detail").value("Resource type must be 1 or 2"));
    }

    @Test
    void downloadStreamsSdkBytesAndUsesSafeAttachmentName() throws Exception {
        doAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write(new byte[] {1, 2, 3});
            return null;
        }).when(resultApi).downloadResource(
                any(ResourceDownloadRequest.class), any(OutputStream.class), any(RequestOptions.class));

        MvcResult result = mockMvc.perform(post("/sample/api/result/download-resource")
                        .header("X-Request-Id", "request-download")
                        .contentType(APPLICATION_JSON)
                        .content("{\"filePath\":\"result/a.jpg\",\"type\":2}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string(CONTENT_DISPOSITION, containsString("a.jpg")))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));

        verify(resultApi).downloadResource(
                any(ResourceDownloadRequest.class), any(OutputStream.class), any(RequestOptions.class));
    }

    @Test
    void downloadNameUsesFinalSegmentAndSanitizesUnsafeCharacters() {
        assertThat(SampleDownloadNames.fromPath("folder\\nested/a\r\n\".jpg"))
                .isEqualTo("a___.jpg");
        assertThat(SampleDownloadNames.fromPath("folder\\nested\\a.jpg"))
                .isEqualTo("a.jpg");
        assertThat(SampleDownloadNames.fromPath("../../"))
                .isEqualTo("station-openapi-resource.bin");
    }

    @Test
    void systemTimeReturnsSdkTextUnchanged() throws Exception {
        when(systemApi.getTimeText(any(RequestOptions.class))).thenReturn("2026-07-13T10:00:00");

        mockMvc.perform(post("/sample/api/system/get-time-text")
                        .header("X-Request-Id", "request-time")
                        .characterEncoding(UTF_8))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"value\":\"2026-07-13T10:00:00\"}"));

        verify(systemApi).getTimeText(any(RequestOptions.class));
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
