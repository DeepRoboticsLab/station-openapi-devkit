package com.deeprobotics.station.openapi.sample.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deeprobotics.station.openapi.sample.service.ResultSampleService;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "station.openapi.endpoint=http://127.0.0.1:9",
        "station.openapi.auth-mode=ACCESS_TOKEN",
        "station.openapi.access-token=sample-contract-token"
})
@AutoConfigureMockMvc
class DownloadTimeoutConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResultSampleService service;

    @Test
    void downloadResponseHasNoAsyncTotalTimeout() throws Exception {
        doAnswer(invocation -> {
            OutputStream output = invocation.getArgument(1);
            output.write(new byte[] {1, 2, 3});
            return null;
        }).when(service).download(any(), any(), any());

        MvcResult result = mockMvc.perform(post("/sample/api/result/download-resource")
                        .contentType(APPLICATION_JSON)
                        .content("{\"filePath\":\"result/a.jpg\",\"type\":2}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertThat(result.getRequest().getAsyncContext().getTimeout()).isZero();
        // 测试等待仍须有界，避免无总时限配置掩盖测试挂起。
        result.getAsyncResult(5_000);
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }
}
