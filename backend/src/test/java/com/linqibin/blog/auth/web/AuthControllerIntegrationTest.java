package com.linqibin.blog.auth.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.linqibin.blog.auth.application.AuthService;
import com.linqibin.blog.auth.infrastructure.InMemoryUserRepository;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;
import com.linqibin.blog.support.AuthTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 认证集成测试：完整安全链路，不使用 addFilters=false，验证 Spring Security 真实行为。
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

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
    void loginWithValidCredentialsReturns200() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@blog.com",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.email").value("admin@blog.com"))
                .andExpect(jsonPath("$.data.displayName").value("Admin"))
                // 密码哈希不应出现在响应中。
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void loginWithInvalidPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@blog.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("邮箱或密码错误"));
    }

    @Test
    void loginWithNonExistentEmailReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nobody@blog.com",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("邮箱或密码错误"));
    }

    @Test
    void loginWithBlankEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unauthorizedAccessToAdminPostsReturns401() throws Exception {
        // 未登录访问管理端文章列表应返回 401。
        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void unauthorizedAccessToAdminMarkdownPreviewReturns401() throws Exception {
        // 未登录访问 Markdown 预览应返回 401。
        mockMvc.perform(post("/api/admin/markdown/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "markdown": "# Hello"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void afterLoginCanAccessAdminPosts() throws Exception {
        // 先登录获取 Session。
        MockHttpSession session = AuthTestSupport.loginAndGetSession(mockMvc);

        // 用同一 Session 访问管理端文章列表，应返回 200。
        mockMvc.perform(get("/api/admin/posts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void afterLoginCanCreateDraft() throws Exception {
        MockHttpSession session = AuthTestSupport.loginAndGetSession(mockMvc);

        mockMvc.perform(post("/api/admin/posts/drafts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test Post",
                                  "markdownContent": "# Hello"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.title").value("Test Post"));
    }

    @Test
    void getCurrentUserReturnsUserInfoAfterLogin() throws Exception {
        MockHttpSession session = AuthTestSupport.loginAndGetSession(mockMvc);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.email").value("admin@blog.com"))
                .andExpect(jsonPath("$.data.displayName").value("Admin"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void getCurrentUserReturns401WithoutLogin() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = AuthTestSupport.loginAndGetSession(mockMvc);

        // 退出登录。
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        // 退出后再访问管理端应返回 401。
        mockMvc.perform(get("/api/admin/posts").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointReturnsPublishedPost() throws Exception {
        // 登录后创建并发布一篇文章。
        MockHttpSession session = AuthTestSupport.loginAndGetSession(mockMvc);

        MvcResult createResult = mockMvc.perform(post("/api/admin/posts/drafts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Published Post",
                                  "markdownContent": "# Hello",
                                  "slug": "published-post"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        // 从响应中提取文章 ID。
        String response = createResult.getResponse().getContentAsString();
        String postId = response.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        // 发布文章。
        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish")
                        .session(session))
                .andExpect(status().isOk());

        // 公开接口应能访问已发布文章。
        mockMvc.perform(get("/api/public/posts/published-post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.title").value("Published Post"));
    }

    @Test
    void publicEndpointReturns404ForDraftPost() throws Exception {
        // 登录后创建一篇草稿但不发布。
        MockHttpSession session = AuthTestSupport.loginAndGetSession(mockMvc);

        mockMvc.perform(post("/api/admin/posts/drafts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Draft Post",
                                  "markdownContent": "# Draft",
                                  "slug": "draft-post"
                                }
                                """))
                .andExpect(status().isCreated());

        // 公开接口访问草稿应返回 404。
        mockMvc.perform(get("/api/public/posts/draft-post"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void publicEndpointReturns404ForUnpublishedPost() throws Exception {
        // 创建并发布一篇文章，然后下线。
        MockHttpSession session = AuthTestSupport.loginAndGetSession(mockMvc);

        MvcResult createResult = mockMvc.perform(post("/api/admin/posts/drafts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Unpublished Post",
                                  "markdownContent": "# Content",
                                  "slug": "unpublished-post"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        String postId = response.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        // 发布再下线。
        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish")
                        .session(session))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/posts/" + postId + "/unpublish")
                        .session(session))
                .andExpect(status().isOk());

        // 公开接口访问已下线文章应返回 404。
        mockMvc.perform(get("/api/public/posts/unpublished-post"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void healthEndpointAccessibleWithoutLogin() throws Exception {
        // 健康检查接口不需要认证。
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void forgotPasswordAlwaysReturnsSameMessage() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nobody@blog.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("如果该邮箱存在，我们已发送重置说明"));
    }

    @Test
    void changePasswordAndLoginWithNewPassword() throws Exception {
        MockHttpSession session = AuthTestSupport.loginAndGetSession(mockMvc);

        mockMvc.perform(put("/api/auth/password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "admin123",
                                  "newPassword": "newpass12"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@blog.com",
                                  "password": "newpass12"
                                }
                                """))
                .andExpect(status().isOk());
    }
}
