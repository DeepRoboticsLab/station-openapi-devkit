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

import com.deeprobotics.station.openapi.sample.config.SampleSafetyProperties;
import com.deeprobotics.station.openapi.sample.safety.DangerousOperationInterceptor;
import com.deeprobotics.station.openapi.sample.service.CameraSampleService;
import com.deeprobotics.station.openapi.sample.service.DogSampleService;
import com.deeprobotics.station.openapi.sample.web.advice.SampleExceptionHandler;
import com.deeprobotics.station.openapi.sample.web.i18n.SampleLocaleResolver;
import com.deeprobotics.station.openapi.sdk.RequestOptions;
import com.deeprobotics.station.openapi.sdk.StationOpenApiClient;
import com.deeprobotics.station.openapi.sdk.api.CameraApi;
import com.deeprobotics.station.openapi.sdk.api.DogApi;
import com.deeprobotics.station.openapi.sdk.model.common.PageResult;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.camera.CameraResetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogAlarmPageRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogChargeControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogControlRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogPositionRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogPositionSetRequest;
import com.deeprobotics.station.openapi.sdk.model.request.dog.DogStateRequest;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogPosition;
import com.deeprobotics.station.openapi.sdk.model.response.dog.DogState;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DogAndCameraControllerTest {

    private DogApi dogApi;
    private CameraApi cameraApi;
    private DogSampleController dogController;
    private CameraSampleController cameraController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StationOpenApiClient client = mock(StationOpenApiClient.class);
        dogApi = mock(DogApi.class);
        cameraApi = mock(CameraApi.class);
        when(client.dog()).thenReturn(dogApi);
        when(client.camera()).thenReturn(cameraApi);
        dogController = new DogSampleController(new DogSampleService(client));
        cameraController = new CameraSampleController(new CameraSampleService(client));
        mockMvc = mockMvc(false);
    }

    @Test
    void dangerousControllersAreBlockedByDefault() {
        Set<String> paths = Set.of(
                "/sample/api/dog/charge-control",
                "/sample/api/dog/control",
                "/sample/api/dog/set-position",
                "/sample/api/camera/control",
                "/sample/api/camera/reset-ptz");

        paths.forEach(path -> org.assertj.core.api.Assertions.assertThatCode(() ->
                        mockMvc.perform(post(path).contentType(APPLICATION_JSON).content("{}"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code")
                                        .value("SAMPLE_DANGEROUS_OPERATION_DISABLED")))
                .doesNotThrowAnyException());

        verifyNoInteractions(dogApi, cameraApi);
    }

    @Test
    void dangerousControllersDelegateWhenExplicitlyEnabled() throws Exception {
        MockMvc enabledMockMvc = mockMvc(true);

        performNoContent(enabledMockMvc, "/sample/api/dog/charge-control",
                "{\"dogCode\":\"dog-001\",\"command\":\"1\"}");
        performNoContent(enabledMockMvc, "/sample/api/dog/control",
                "{\"dogCode\":\"dog-001\",\"type\":2,\"command\":99,\"value\":\"0.2\",\"speed\":1}");
        performNoContent(enabledMockMvc, "/sample/api/dog/set-position",
                "{\"dogCode\":\"dog-001\",\"edgeCode\":\"edge-001\",\"x\":\"1.2\",\"y\":\"2.3\"}");
        performNoContent(enabledMockMvc, "/sample/api/camera/control",
                "{\"cameraCode\":\"camera-001\",\"ctrlType\":3,\"command\":1,\"value\":1}");
        performNoContent(enabledMockMvc, "/sample/api/camera/reset-ptz",
                "{\"cameraCode\":\"camera-001\"}");

        verify(dogApi).chargeControl(any(DogChargeControlRequest.class), any(RequestOptions.class));
        verify(dogApi).control(any(DogControlRequest.class), any(RequestOptions.class));
        verify(dogApi).setPosition(any(DogPositionSetRequest.class), any(RequestOptions.class));
        verify(cameraApi).control(any(CameraControlRequest.class), any(RequestOptions.class));
        verify(cameraApi).resetPtz(any(CameraResetRequest.class), any(RequestOptions.class));
    }

    @Test
    void safeDogQueriesDelegateAndUseHttpResponseSemantics() throws Exception {
        when(dogApi.getState(any(DogStateRequest.class), any(RequestOptions.class)))
                .thenReturn(new DogState("dog-001", "X30", List.of()));
        when(dogApi.pageAlarms(any(DogAlarmPageRequest.class), any(RequestOptions.class)))
                .thenReturn(new PageResult<>(List.of(), 0L, 1L, 10L, 0L));
        when(dogApi.getPosition(any(DogPositionRequest.class), any(RequestOptions.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/sample/api/dog/get-state")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dogCode\":\"dog-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dogCode").value("dog-001"));
        mockMvc.perform(post("/sample/api/dog/page-alarms")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.pageNo").value(1));
        mockMvc.perform(post("/sample/api/dog/get-position")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dogCode\":\"dog-001\"}"))
                .andExpect(status().isNoContent());

        verify(dogApi).getState(any(DogStateRequest.class), any(RequestOptions.class));
        verify(dogApi).pageAlarms(any(DogAlarmPageRequest.class), any(RequestOptions.class));
        verify(dogApi).getPosition(any(DogPositionRequest.class), any(RequestOptions.class));
    }

    @Test
    void dogPositionReturnsBodyWhenSdkHasData() throws Exception {
        when(dogApi.getPosition(any(DogPositionRequest.class), any(RequestOptions.class)))
                .thenReturn(Optional.of(new DogPosition(
                        "dog-001", "Robot Dog", "1", "2", "3", "0.5", "2026-07-13 10:00:00")));

        mockMvc.perform(post("/sample/api/dog/get-position")
                        .contentType(APPLICATION_JSON)
                        .content("{\"dogCode\":\"dog-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dogCode").value("dog-001"))
                .andExpect(jsonPath("$.yaw").value("0.5"));
    }

    private MockMvc mockMvc(boolean dangerousOperationsEnabled) {
        SampleSafetyProperties properties = new SampleSafetyProperties();
        properties.setDangerousOperationsEnabled(dangerousOperationsEnabled);
        return MockMvcBuilders.standaloneSetup(dogController, cameraController)
                .setControllerAdvice(new SampleExceptionHandler(messageSource()))
                .setLocaleResolver(new SampleLocaleResolver())
                .addInterceptors(new DangerousOperationInterceptor(properties))
                .build();
    }

    private static void performNoContent(MockMvc mvc, String path, String body) throws Exception {
        mvc.perform(post(path)
                        .header("X-Request-Id", "request-dangerous")
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
