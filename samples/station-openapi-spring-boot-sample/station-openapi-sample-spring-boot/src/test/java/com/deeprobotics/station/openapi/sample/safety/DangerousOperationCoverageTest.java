package com.deeprobotics.station.openapi.sample.safety;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deeprobotics.station.openapi.sample.service.CameraSampleService;
import com.deeprobotics.station.openapi.sample.service.DogSampleService;
import com.deeprobotics.station.openapi.sample.service.InventorySampleService;
import com.deeprobotics.station.openapi.sample.service.ResultSampleService;
import com.deeprobotics.station.openapi.sample.service.SystemSampleService;
import com.deeprobotics.station.openapi.sample.service.TaskSampleService;
import com.deeprobotics.station.openapi.sample.service.TaskTemplateSampleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest(properties = {
        "station.openapi.endpoint=http://127.0.0.1:9",
        "station.openapi.auth-mode=ACCESS_TOKEN",
        "station.openapi.access-token=sample-danger-token"
})
@AutoConfigureMockMvc
class DangerousOperationCoverageTest {

    private static final Set<String> DANGEROUS_PATHS = Set.of(
            "/sample/api/dog/charge-control",
            "/sample/api/dog/control",
            "/sample/api/dog/set-position",
            "/sample/api/camera/control",
            "/sample/api/camera/reset-ptz",
            "/sample/api/task-template/create",
            "/sample/api/task-template/switch-status",
            "/sample/api/task-template/delete-by-codes",
            "/sample/api/task/issue",
            "/sample/api/task/control",
            "/sample/api/task/batch-cancel");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @MockitoBean
    private InventorySampleService inventoryService;

    @MockitoBean
    private DogSampleService dogService;

    @MockitoBean
    private CameraSampleService cameraService;

    @MockitoBean
    private TaskTemplateSampleService taskTemplateService;

    @MockitoBean
    private TaskSampleService taskService;

    @MockitoBean
    private ResultSampleService resultService;

    @MockitoBean
    private SystemSampleService systemService;

    @Test
    void dangerousAnnotationPinsExactlyElevenPaths() {
        Set<String> actualPaths = new LinkedHashSet<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handlerMethod) -> {
            if (AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getMethod(), DangerousSampleOperation.class) != null) {
                actualPaths.addAll(mapping.getPatternValues());
            }
        });

        assertThat(actualPaths).containsExactlyInAnyOrderElementsOf(DANGEROUS_PATHS);
    }

    @Test
    void dangerousPathsAreBlockedBeforeAnyServiceCall() throws Exception {
        for (String path : DANGEROUS_PATHS) {
            mockMvc.perform(post(path).contentType(APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("SAMPLE_DANGEROUS_OPERATION_DISABLED"));
        }

        verifyNoInteractions(inventoryService, dogService, cameraService, taskTemplateService,
                taskService, resultService, systemService);
    }

    @Test
    void dangerousOperationsExposeBilingualWarningsAndForbiddenExamples() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode paths = objectMapper.readTree(content).path("paths");

        for (String path : DANGEROUS_PATHS) {
            JsonNode operation = paths.path(path).path("post");
            assertThat(operation.path("description").asText())
                    .contains("危险操作")
                    .contains("Dangerous operation");
            JsonNode examples = operation.path("responses").path("403")
                    .path("content").path("application/problem+json").path("examples");
            assertThat(examples.propertyStream().map(Map.Entry::getKey))
                    .containsExactlyInAnyOrder("zh_CN", "en_US");
        }
    }
}
