package com.deeprobotics.station.openapi.sample.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deeprobotics.station.openapi.sample.config.SampleSafetyProperties;
import com.deeprobotics.station.openapi.sample.safety.DangerousOperationInterceptor;
import com.deeprobotics.station.openapi.sample.service.TaskSampleService;
import com.deeprobotics.station.openapi.sample.service.TaskTemplateSampleService;
import com.deeprobotics.station.openapi.sample.web.advice.SampleExceptionHandler;
import com.deeprobotics.station.openapi.sample.web.i18n.SampleLocaleResolver;
import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.api.TaskApi;
import com.deeprobotics.station.openapi.sdk.api.TaskTemplateApi;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskBatchCancelRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskIssueRequest;
import com.deeprobotics.station.openapi.sdk.model.request.task.TaskPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateCreateRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateDeleteRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplateGetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.tasktemplate.TaskTemplatePageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.tasktemplate.TaskTemplateInfo;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TaskControllersTest {

    private TaskTemplateApi taskTemplateApi;
    private TaskApi taskApi;
    private TaskTemplateSampleController taskTemplateController;
    private TaskSampleController taskController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StationOpenApiClient client = mock(StationOpenApiClient.class);
        taskTemplateApi = mock(TaskTemplateApi.class);
        taskApi = mock(TaskApi.class);
        when(client.taskTemplate()).thenReturn(taskTemplateApi);
        when(client.task()).thenReturn(taskApi);
        taskTemplateController = new TaskTemplateSampleController(new TaskTemplateSampleService(client));
        taskController = new TaskSampleController(new TaskSampleService(client));
        mockMvc = mockMvc(false);
    }

    @Test
    void dangerousTaskOperationsAreBlockedByDefault() {
        Set<String> paths = Set.of(
                "/sample/api/task-template/create",
                "/sample/api/task-template/delete-by-codes",
                "/sample/api/task/issue",
                "/sample/api/task/control",
                "/sample/api/task/batch-cancel");

        paths.forEach(path -> org.assertj.core.api.Assertions.assertThatCode(() ->
                        mockMvc.perform(post(path).contentType(APPLICATION_JSON).content("{}"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code")
                                        .value("SAMPLE_DANGEROUS_OPERATION_DISABLED")))
                .doesNotThrowAnyException());
        verifyNoInteractions(taskTemplateApi, taskApi);
    }

    @Test
    void dangerousTaskOperationsDelegateWhenEnabled() throws Exception {
        when(taskTemplateApi.create(any(TaskTemplateCreateRequest.class), any(RequestOptions.class)))
                .thenReturn("T-1");
        when(taskApi.issue(any(TaskIssueRequest.class), any(RequestOptions.class))).thenReturn(null);
        when(taskApi.control(any(TaskControlRequest.class), any(RequestOptions.class))).thenReturn("paused");
        MockMvc enabled = mockMvc(true);

        enabled.perform(post("/sample/api/task-template/create")
                        .contentType(APPLICATION_JSON)
                        .content("{\"taskName\":\"Daily\",\"pointCodes\":[\"P-1\"],\"endAction\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("T-1"));
        performNoContent(enabled, "/sample/api/task-template/delete-by-codes",
                "{\"taskCodes\":[\"T-1\"]}");
        enabled.perform(post("/sample/api/task/issue")
                        .contentType(APPLICATION_JSON)
                        .content("{\"taskCode\":\"T-1\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"value\":null}"));
        enabled.perform(post("/sample/api/task/control")
                        .contentType(APPLICATION_JSON)
                        .content("{\"taskExecuteCode\":\"E-1\",\"command\":\"99\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("paused"));
        performNoContent(enabled, "/sample/api/task/batch-cancel",
                "{\"taskExecuteCodeList\":[\"E-1\",\"E-2\"]}");

        verify(taskTemplateApi).create(any(TaskTemplateCreateRequest.class), any(RequestOptions.class));
        verify(taskTemplateApi).deleteByCodes(any(TaskTemplateDeleteRequest.class), any(RequestOptions.class));
        verify(taskApi).issue(any(TaskIssueRequest.class), any(RequestOptions.class));
        verify(taskApi).control(any(TaskControlRequest.class), any(RequestOptions.class));
        verify(taskApi).batchCancel(any(TaskBatchCancelRequest.class), any(RequestOptions.class));
    }

    @Test
    void safeTaskQueriesDelegateAndReturnSerializablePages() throws Exception {
        TaskTemplateInfo info = mock(TaskTemplateInfo.class);
        when(info.taskCode()).thenReturn("T-1");
        when(taskTemplateApi.getByCode(any(TaskTemplateGetRequest.class), any(RequestOptions.class)))
                .thenReturn(info);
        when(taskTemplateApi.page(any(TaskTemplatePageRequest.class), any(RequestOptions.class)))
                .thenReturn(new PageResult<>(List.of(), 0L, 1L, 10L, 0L));
        when(taskApi.page(any(TaskPageRequest.class), any(RequestOptions.class)))
                .thenReturn(new PageResult<>(List.of(), 0L, 1L, 10L, 0L));

        mockMvc.perform(post("/sample/api/task-template/get-by-code")
                        .contentType(APPLICATION_JSON)
                        .content("{\"taskCode\":\"T-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskCode").value("T-1"));
        mockMvc.perform(post("/sample/api/task-template/page")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray());
        mockMvc.perform(post("/sample/api/task/page")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageNo").value(1));

        verify(taskTemplateApi).getByCode(any(TaskTemplateGetRequest.class), any(RequestOptions.class));
        verify(taskTemplateApi).page(any(TaskTemplatePageRequest.class), any(RequestOptions.class));
        verify(taskApi).page(any(TaskPageRequest.class), any(RequestOptions.class));
    }

    private MockMvc mockMvc(boolean dangerousEnabled) {
        SampleSafetyProperties properties = new SampleSafetyProperties();
        properties.setDangerousOperationsEnabled(dangerousEnabled);
        return MockMvcBuilders.standaloneSetup(taskTemplateController, taskController)
                .setControllerAdvice(new SampleExceptionHandler(messageSource()))
                .setLocaleResolver(new SampleLocaleResolver())
                .addInterceptors(new DangerousOperationInterceptor(properties))
                .build();
    }

    private static void performNoContent(MockMvc mvc, String path, String body) throws Exception {
        mvc.perform(post(path)
                        .header("X-Request-Id", "request-task")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
