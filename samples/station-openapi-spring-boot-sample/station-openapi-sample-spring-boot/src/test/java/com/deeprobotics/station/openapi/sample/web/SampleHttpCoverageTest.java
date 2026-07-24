package com.deeprobotics.station.openapi.sample.web;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "station.openapi.endpoint=http://127.0.0.1:9",
        "station.openapi.auth-mode=ACCESS_TOKEN",
        "station.openapi.access-token=sample-contract-token"
})
@AutoConfigureMockMvc
class SampleHttpCoverageTest {

    private static final Map<String, String> EXPECTED_OPERATIONS = Map.ofEntries(
            entry("/sample/api/inventory/page-dogs", "inventory.pageDogs"),
            entry("/sample/api/inventory/list-maps", "inventory.listMaps"),
            entry("/sample/api/inventory/page-streams", "inventory.pageStreams"),
            entry("/sample/api/inventory/page-points", "inventory.pagePoints"),
            entry("/sample/api/inventory/page-maintain-areas", "inventory.pageMaintainAreas"),
            entry("/sample/api/inventory/list-road-maps", "inventory.listRoadMaps"),
            entry("/sample/api/inventory/list-docks", "inventory.listDocks"),
            entry("/sample/api/inventory/list-nodes", "inventory.listNodes"),
            entry("/sample/api/dog/get-state", "dog.getState"),
            entry("/sample/api/dog/charge-control", "dog.chargeControl"),
            entry("/sample/api/dog/control", "dog.control"),
            entry("/sample/api/dog/set-position", "dog.setPosition"),
            entry("/sample/api/dog/page-alarms", "dog.pageAlarms"),
            entry("/sample/api/dog/get-position", "dog.getPosition"),
            entry("/sample/api/camera/control", "camera.control"),
            entry("/sample/api/camera/reset-ptz", "camera.resetPtz"),
            entry("/sample/api/task-template/create", "taskTemplate.create"),
            entry("/sample/api/task-template/delete-by-codes", "taskTemplate.deleteByCodes"),
            entry("/sample/api/task-template/get-by-code", "taskTemplate.getByCode"),
            entry("/sample/api/task-template/page", "taskTemplate.page"),
            entry("/sample/api/task/issue", "task.issue"),
            entry("/sample/api/task/control", "task.control"),
            entry("/sample/api/task/batch-cancel", "task.batchCancel"),
            entry("/sample/api/task/page", "task.page"),
            entry("/sample/api/result/page-task-point-results", "result.pageTaskPointResults"),
            entry("/sample/api/result/download-resource", "result.downloadResource"),
            entry("/sample/api/system/get-time-text", "system.getTimeText"));

    private static final Set<String> EXPECTED_TAGS = Set.of(
            "Inventory", "Dog", "Camera", "Task Template", "Task", "Result", "System");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void openApiPinsAllCustomerVisibleOperationsAndSharedResponses() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode paths = document.path("paths");
        Map<String, String> actualOperations = new LinkedHashMap<>();
        Set<String> actualTags = new LinkedHashSet<>();

        paths.properties().forEach(pathEntry -> {
            if (!pathEntry.getKey().startsWith("/sample/api/")) {
                return;
            }
            JsonNode pathItem = pathEntry.getValue();
            assertThat(pathItem.propertyStream().map(Map.Entry::getKey))
                    .as("每个 Sample 路径只能暴露 POST")
                    .containsExactly("post");
            JsonNode operation = pathItem.path("post");
            actualOperations.put(pathEntry.getKey(), operation.path("operationId").asText());
            operation.path("tags").forEach(tag -> actualTags.add(tag.asText()));

            assertThat(hasOptionalRequestIdHeader(operation))
                    .as("%s 必须提供可选 X-Request-Id", pathEntry.getKey())
                    .isTrue();
            assertThat(exampleNames(operation.path("responses").path("400")))
                    .as("%s 的 400 响应必须包含中英文示例", pathEntry.getKey())
                    .containsExactlyInAnyOrder("zh_CN", "en_US");
        });

        assertThat(actualOperations).containsExactlyInAnyOrderEntriesOf(EXPECTED_OPERATIONS);
        assertThat(actualTags).containsExactlyInAnyOrderElementsOf(EXPECTED_TAGS);
    }

    private JsonNode openApiDocument() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
    }

    private static boolean hasOptionalRequestIdHeader(JsonNode operation) {
        return StreamSupport.stream(operation.path("parameters").spliterator(), false)
                .anyMatch(parameter -> "X-Request-Id".equals(parameter.path("name").asText())
                        && "header".equals(parameter.path("in").asText())
                        && !parameter.path("required").asBoolean(false));
    }

    private static Set<String> exampleNames(JsonNode response) {
        JsonNode examples = response.path("content").path("application/problem+json").path("examples");
        Set<String> names = new LinkedHashSet<>();
        examples.propertyStream().map(Map.Entry::getKey).forEach(names::add);
        return names;
    }
}
