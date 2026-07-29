package com.linqibin.blog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import com.linqibin.blog.common.request.RequestIdUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BackendApplicationTests.TestApiController.class)
class BackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health").header(RequestIdUtils.REQUEST_ID_HEADER, "health-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "health-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("health-request-id"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void exceptionResponseUsesUnifiedStructure() throws Exception {
        mockMvc.perform(get("/api/test/error").header(RequestIdUtils.REQUEST_ID_HEADER, "error-request-id"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "error-request-id"))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                .andExpect(jsonPath("$.requestId").value("error-request-id"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void validationResponseUsesUnifiedStructure() throws Exception {
        mockMvc.perform(post("/api/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "validation-request-id"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "validation-request-id"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("请求参数校验失败"))
                .andExpect(jsonPath("$.requestId").value("validation-request-id"))
                .andExpect(jsonPath("$.data.fieldErrors.title").value("must not be blank"));
    }

    @RestController
    @RequestMapping("/api/test")
    static class TestApiController {

        @GetMapping("/error")
        public ErrorResponse error() {
            throw new IllegalStateException("boom");
        }

        @PostMapping("/validation")
        public ValidationResponse validate(@Valid @RequestBody ValidationRequest request) {
            return new ValidationResponse(request.title());
        }
    }

    record ErrorResponse(String value) {
    }

    record ValidationRequest(@NotBlank String title) {
    }

    record ValidationResponse(String title) {
    }
}
