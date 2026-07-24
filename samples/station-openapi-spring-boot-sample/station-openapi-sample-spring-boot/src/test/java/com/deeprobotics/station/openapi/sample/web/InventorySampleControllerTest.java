package com.deeprobotics.station.openapi.sample.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deeprobotics.station.openapi.sample.service.InventorySampleService;
import com.deeprobotics.station.openapi.sample.web.advice.SampleExceptionHandler;
import com.deeprobotics.station.openapi.sample.web.i18n.SampleLocaleResolver;
import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.api.InventoryApi;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DevicePointPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DockListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.DogPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.MaintainAreaPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.MapListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.NodeListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.RoadMapListRequest;
import com.deeprobotics.station.openapi.sdk.model.request.inventory.StreamPageRequest;
import com.deeprobotics.station.openapi.sdk.model.response.inventory.CameraStreamInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InventorySampleControllerTest {

    private InventoryApi inventoryApi;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StationOpenApiClient client = mock(StationOpenApiClient.class);
        inventoryApi = mock(InventoryApi.class);
        when(client.inventory()).thenReturn(inventoryApi);
        InventorySampleService service = new InventorySampleService(client);
        mockMvc = MockMvcBuilders.standaloneSetup(new InventorySampleController(service))
                .setControllerAdvice(new SampleExceptionHandler(messageSource()))
                .setLocaleResolver(new SampleLocaleResolver())
                .build();
    }

    @Test
    void invalidRoadMapInputReturnsLocalizedProblemWithoutCallingService() throws Exception {
        mockMvc.perform(post("/sample/api/inventory/list-road-maps?lang=en_US")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dogCode\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail")
                        .value("Robot dog code dogCode is required when querying road map edges."))
                .andExpect(jsonPath("$.code").value("SAMPLE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.field").value("dogCode"));

        verifyNoInteractions(inventoryApi);
    }

    @Test
    void pageStreamsResponseNeverContainsCameraPassword() throws Exception {
        CameraStreamInfo stream = new CameraStreamInfo(
                "front", "camera-001", "dog-001", "vendor", "model", "patrol", "visible",
                "127.0.0.1", "554", "viewer", "secret-password", "1", "main", "rtsp://camera");
        when(inventoryApi.pageStreams(any(StreamPageRequest.class), any(RequestOptions.class)))
                .thenReturn(new PageResult<>(List.of(stream), 1L, null, null, null));

        mockMvc.perform(post("/sample/api/inventory/page-streams")
                        .header("X-Request-Id", "request-streams")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.records[0].code").value("camera-001"))
                .andExpect(jsonPath("$.records[0].streamUrl").value("rtsp://camera"))
                .andExpect(jsonPath("$.records[0].password").doesNotExist());

        verify(inventoryApi).pageStreams(any(StreamPageRequest.class), any(RequestOptions.class));
    }

    @Test
    void remainingInventoryEndpointsDelegateSuccessfully() throws Exception {
        when(inventoryApi.pageDogs(any(DogPageRequest.class), any(RequestOptions.class)))
                .thenReturn(new PageResult<>(List.of(), 0L, 1L, 10L, 0L));
        when(inventoryApi.listMaps(any(MapListRequest.class), any(RequestOptions.class))).thenReturn(List.of());
        when(inventoryApi.pagePoints(any(DevicePointPageRequest.class), any(RequestOptions.class)))
                .thenReturn(new PageResult<>(List.of(), 0L, 1L, 10L, 0L));
        when(inventoryApi.pageMaintainAreas(any(MaintainAreaPageRequest.class), any(RequestOptions.class)))
                .thenReturn(new PageResult<>(List.of(), 0L, 1L, 10L, 0L));
        when(inventoryApi.listRoadMaps(any(RoadMapListRequest.class), any(RequestOptions.class)))
                .thenReturn(List.of());
        when(inventoryApi.listDocks(any(DockListRequest.class), any(RequestOptions.class))).thenReturn(List.of());
        when(inventoryApi.listNodes(any(NodeListRequest.class), any(RequestOptions.class))).thenReturn(List.of());

        performOk("/sample/api/inventory/page-dogs", "{}");
        performOk("/sample/api/inventory/list-maps", "{}");
        performOk("/sample/api/inventory/page-points", "{}");
        performOk("/sample/api/inventory/page-maintain-areas", "{}");
        performOk("/sample/api/inventory/list-road-maps", "{\"dogCode\":\"dog-001\"}");
        performOk("/sample/api/inventory/list-docks", "{\"dogCode\":\"dog-001\"}");
        performOk("/sample/api/inventory/list-nodes", "{\"dogCode\":\"dog-001\"}");

        verify(inventoryApi).pageDogs(any(DogPageRequest.class), any(RequestOptions.class));
        verify(inventoryApi).listMaps(any(MapListRequest.class), any(RequestOptions.class));
        verify(inventoryApi).pagePoints(any(DevicePointPageRequest.class), any(RequestOptions.class));
        verify(inventoryApi).pageMaintainAreas(
                any(MaintainAreaPageRequest.class), any(RequestOptions.class));
        verify(inventoryApi).listRoadMaps(any(RoadMapListRequest.class), any(RequestOptions.class));
        verify(inventoryApi).listDocks(any(DockListRequest.class), any(RequestOptions.class));
        verify(inventoryApi).listNodes(any(NodeListRequest.class), any(RequestOptions.class));
    }

    private void performOk(String path, String body) throws Exception {
        mockMvc.perform(post(path).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
