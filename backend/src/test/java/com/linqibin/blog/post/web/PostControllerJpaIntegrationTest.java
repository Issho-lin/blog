package com.linqibin.blog.post.web;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.linqibin.blog.common.request.RequestIdUtils;
import com.linqibin.blog.post.domain.PostStatus;
import com.linqibin.blog.post.infrastructure.persistence.PostEntity;
import com.linqibin.blog.post.infrastructure.persistence.SpringDataPostRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("jpa")
@TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=blog",
        "DB_USER=blog",
        "DB_PASSWORD=blog"
})
class PostControllerJpaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataPostRepository springDataPostRepository;

    @BeforeEach
    void setUp() {
        springDataPostRepository.deleteAll();
    }

    @Test
    void createDraftEndpointPersistsEntityToPostgreSql() throws Exception {
        mockMvc.perform(post("/api/admin/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hello JPA Controller",
                                  "markdownContent": "# Hello JPA"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "create-jpa-request-id"))
                .andExpect(status().isCreated())
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "create-jpa-request-id"))
                .andExpect(jsonPath("$.data.slug").value("hello-jpa-controller"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        PostEntity savedEntity = springDataPostRepository.findBySlug("hello-jpa-controller").orElseThrow();

        assertThat(savedEntity.getTitle()).isEqualTo("Hello JPA Controller");
        assertThat(savedEntity.getMarkdownContent()).isEqualTo("# Hello JPA");
        assertThat(savedEntity.getStatus()).isEqualTo(PostStatus.DRAFT);
    }

    @Test
    void updateEndpointWithBlankSlugRegeneratesSlugAndPersistsItToPostgreSql() throws Exception {
        UUID postId = createDraft("Original Title", "# old");

        mockMvc.perform(put("/api/admin/posts/" + postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "New Url Title",
                                  "markdownContent": "# new",
                                  "slug": ""
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "update-jpa-request-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "update-jpa-request-id"))
                .andExpect(jsonPath("$.data.slug").value("new-url-title"));

        PostEntity savedEntity = springDataPostRepository.findById(postId).orElseThrow();

        assertThat(savedEntity.getTitle()).isEqualTo("New Url Title");
        assertThat(savedEntity.getSlug()).isEqualTo("new-url-title");
        assertThat(savedEntity.getMarkdownContent()).isEqualTo("# new");
    }

    @Test
    void trashAndRestoreEndpointsPersistLifecycleFieldsToPostgreSql() throws Exception {
        UUID postId = createDraft("Trash Me", "# content");
        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/posts/" + postId + "/unpublish"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/posts/" + postId + "/trash")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "trash-jpa-request-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "trash-jpa-request-id"))
                .andExpect(jsonPath("$.data.status").value("TRASHED"));

        PostEntity trashedEntity = springDataPostRepository.findById(postId).orElseThrow();
        assertThat(trashedEntity.getStatus()).isEqualTo(PostStatus.TRASHED);
        assertThat(trashedEntity.getPreviousStatusBeforeTrash()).isEqualTo(PostStatus.UNPUBLISHED);
        assertThat(trashedEntity.getPublishedAt()).isNotNull();

        mockMvc.perform(post("/api/admin/posts/" + postId + "/restore")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "restore-jpa-request-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "restore-jpa-request-id"))
                .andExpect(jsonPath("$.data.status").value("UNPUBLISHED"));

        PostEntity restoredEntity = springDataPostRepository.findById(postId).orElseThrow();
        assertThat(restoredEntity.getStatus()).isEqualTo(PostStatus.UNPUBLISHED);
        assertThat(restoredEntity.getPreviousStatusBeforeTrash()).isNull();
        assertThat(restoredEntity.getPublishedAt()).isNotNull();
    }

    @Test
    void publicGetEndpointReadsPostFromPostgreSql() throws Exception {
        UUID postId = createDraft("Read From DB", "# content");
        // 发布后公开接口才能读取。
        mockMvc.perform(post("/api/admin/posts/" + postId + "/publish"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/posts/read-from-db")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "public-read-jpa-request-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "public-read-jpa-request-id"))
                .andExpect(jsonPath("$.data.title").value("Read From DB"))
                .andExpect(jsonPath("$.data.slug").value("read-from-db"));
    }

    private UUID createDraft(String title, String markdownContent) throws Exception {
        mockMvc.perform(post("/api/admin/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "markdownContent": "%s"
                                }
                                """.formatted(title, markdownContent)))
                .andExpect(status().isCreated());

        String expectedSlug = title.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return springDataPostRepository.findBySlug(expectedSlug)
                .orElseThrow()
                .getId();
    }
}
