package com.linqibin.blog.support;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 认证相关集成测试的公共常量与登录辅助方法，避免各测试类重复拼装登录请求。
public final class AuthTestSupport {

    public static final String DEFAULT_ADMIN_EMAIL = "admin@blog.com";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    public static final String DEFAULT_ADMIN_NAME = "Admin";

    private AuthTestSupport() {
    }

    public static MockHttpSession loginAndGetSession(MockMvc mockMvc) throws Exception {
        return loginAndGetSession(mockMvc, DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
    }

    public static MockHttpSession loginAndGetSession(
            MockMvc mockMvc,
            String email,
            String password
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }
}
