package com.deeprobotics.station.openapi.sample.web;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deeprobotics.station.openapi.sample.service.InventorySampleService;
import com.deeprobotics.station.openapi.sample.web.view.CameraStreamView;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.CameraStreamInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "station.openapi.endpoint=http://127.0.0.1:9",
        "station.openapi.auth-mode=ACCESS_TOKEN",
        "station.openapi.access-token=sample-camera-token"
})
@AutoConfigureMockMvc
class CameraStreamSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventorySampleService inventoryService;

    @Test
    void cameraPasswordIsAbsentFromViewJsonAndOpenApiSchema() throws Exception {
        CameraStreamInfo source = new CameraStreamInfo(
                "Front camera", "camera-001", "dog-001", "vendor", "model", "patrol", "main",
                "192.0.2.10", "554", "viewer", "camera-secret", "1", "main",
                "rtsp://viewer:synthetic-stream-credential@example.invalid/live");
        CameraStreamView safeView = CameraStreamView.from(source);
        when(inventoryService.pageStreams(any(), any()))
                .thenReturn(new PageResult<>(List.of(safeView), 1L, 1L, 10L, 1L));

        assertThat(Arrays.stream(CameraStreamView.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("password");

        String pageStreamsResponseJson = mockMvc.perform(post("/sample/api/inventory/page-streams")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(UTF_8);
        assertThat(pageStreamsResponseJson)
                .doesNotContain("password")
                .doesNotContain("synthetic-stream-credential")
                .doesNotContain("camera-secret");

        String openApiJson = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode cameraStreamSchema = objectMapper.readTree(openApiJson)
                .path("components").path("schemas").path("CameraStreamView");
        assertThat(cameraStreamSchema.path("properties").has("password")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "rtsp://viewer:synthetic-stream-credential@example.invalid/live",
            "https://example.invalid/live?ticket=synthetic-stream-credential",
            "https://example.invalid/live#synthetic-stream-credential",
            "rtsp://viewer:synthetic stream credential@example.invalid/live"
    })
    void viewOmitsUrlsThatMayCarryCredentials(String streamUrl) throws Exception {
        CameraStreamInfo source = new CameraStreamInfo(
                "Front camera", "camera-001", "dog-001", null, null, null, null,
                "192.0.2.10", "554", "viewer", "camera-secret", "1", "main", streamUrl);
        CameraStreamView view = CameraStreamView.from(source);
        assertThat(view.streamUrl()).isNull();
        assertThat(objectMapper.writeValueAsString(view)).doesNotContain("synthetic-stream-credential");
        assertThat(view.toString()).doesNotContain("synthetic-stream-credential");
        assertThat(source.streamUrl()).isEqualTo(streamUrl);
    }
}
