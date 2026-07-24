package com.deeprobotics.station.openapi.sample.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deeprobotics.station.openapi.sample.service.DogSampleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "station.openapi.endpoint=http://127.0.0.1:9",
        "station.openapi.auth-mode=ACCESS_TOKEN",
        "station.openapi.access-token=sample-i18n-token"
})
@AutoConfigureMockMvc
class ValidationI18nTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DogSampleService dogService;

    @Test
    void langParameterOverridesAcceptLanguageForChinese() throws Exception {
        mockMvc.perform(post("/sample/api/dog/get-state?lang=zh_CN")
                        .header("Accept-Language", "en-US")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("参数校验失败"))
                .andExpect(jsonPath("$.detail").value("查询四足狗状态时 dogCode 不能为空"))
                .andExpect(jsonPath("$.code").value("SAMPLE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.field").value("dogCode"));
    }

    @Test
    void langParameterOverridesAcceptLanguageForEnglish() throws Exception {
        mockMvc.perform(post("/sample/api/dog/get-state?lang=en_US")
                        .header("Accept-Language", "zh-CN")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail")
                        .value("dogCode is required when querying robot dog status."))
                .andExpect(jsonPath("$.code").value("SAMPLE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.field").value("dogCode"));
    }
}
