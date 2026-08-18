package com.linqibin.blog.comment.web;

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
import com.linqibin.blog.comment.infrastructure.InMemoryCommentRepository;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;
import com.linqibin.blog.support.AuthTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryUserRepository userRepository;

    @Autowired
    private InMemoryPostRepository postRepository;

    @Autowired
    private InMemoryCommentRepository commentRepository;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository.clear();
        postRepository.clear();
        commentRepository.clear();
        authService.initializeDefaultAdmin(
                AuthTestSupport.DEFAULT_ADMIN_EMAIL,
                AuthTestSupport.DEFAULT_ADMIN_PASSWORD,
                AuthTestSupport.DEFAULT_ADMIN_NAME
        );
    }

    @Test
    void visitorCanCommentOnPublishedPost() throws Exception {
        MockHttpSession session = AuthTestSupport.loginAndGetSession(mockMvc);
        MvcResult createResult = mockMvc.perform(post("/api/admin/posts/drafts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Commented Post",
                                  "markdownContent": "# Hello",
                                  "slug": "commented-post"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String postId = createResult.getResponse().getContentAsString()
                .replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/public/posts/commented-post/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorName": "访客",
                                  "content": "写得好"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authorName").value("访客"));

        mockMvc.perform(get("/api/public/posts/commented-post/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
