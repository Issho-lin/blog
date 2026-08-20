package com.linqibin.blog.auth.web;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.linqibin.blog.auth.application.AuthService;
import com.linqibin.blog.auth.infrastructure.InMemoryUserRepository;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;
import com.linqibin.blog.support.AuthTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 验证导入、导出和媒体上传接口在未登录时被 Spring Security 拦截。
@SpringBootTest
@AutoConfigureMockMvc
class AdminProtectedEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryUserRepository userRepository;

    @Autowired
    private InMemoryPostRepository postRepository;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository.clear();
        postRepository.clear();
        authService.initializeDefaultAdmin(
                AuthTestSupport.DEFAULT_ADMIN_EMAIL,
                AuthTestSupport.DEFAULT_ADMIN_PASSWORD,
                AuthTestSupport.DEFAULT_ADMIN_NAME
        );
    }

    @Test
    void unauthorizedImportReturns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "post.md",
                "text/markdown",
                "# content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/imports").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unauthorizedExportReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/posts/00000000-0000-0000-0000-000000000001/export"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unauthorizedMediaUploadReturns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "image/png",
                new byte[100]
        );

        mockMvc.perform(multipart("/api/admin/media/images").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unauthorizedAiSummarizeReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/ai/summarize")
                        .contentType("application/json")
                        .content("{\"markdown\":\"正文\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
