package com.linqibin.blog.post.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.linqibin.blog.common.request.RequestIdUtils;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryPostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository.clear();
    }

    @Test
    void createDraftEndpointReturnsCreatedPost() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hello Spring Boot",
                                  "markdownContent": "# Hello"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "create-post-request-id"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "create-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("create-post-request-id"))
                .andExpect(jsonPath("$.data.title").value("Hello Spring Boot"))
                .andExpect(jsonPath("$.data.slug").value("hello-spring-boot"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void getPostBySlugEndpointReturnsSavedPost() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Read Me",
                                  "markdownContent": "# content"
                                }
                                """));

        mockMvc.perform(get("/api/public/posts/read-me")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "get-post-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "get-post-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("get-post-request-id"))
                .andExpect(jsonPath("$.data.title").value("Read Me"))
                .andExpect(jsonPath("$.data.slug").value("read-me"));
    }

    @Test
    void createDraftWithDuplicateRequestedSlugReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "First Post",
                          "markdownContent": "# first",
                          "slug": "custom-post"
                        }
                        """));

        mockMvc.perform(post("/api/admin/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Second Post",
                                  "markdownContent": "# second",
                                  "slug": "custom-post"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "duplicate-slug-request-id"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "duplicate-slug-request-id"))
                .andExpect(jsonPath("$.code").value("DUPLICATE_SLUG"))
                .andExpect(jsonPath("$.message").value("slug 已存在: custom-post"))
                .andExpect(jsonPath("$.requestId").value("duplicate-slug-request-id"));
    }

    @Test
    void getMissingPostBySlugReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/public/posts/not-exists")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "missing-post-request-id"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "missing-post-request-id"))
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("文章不存在: not-exists"))
                .andExpect(jsonPath("$.requestId").value("missing-post-request-id"));
    }
}
