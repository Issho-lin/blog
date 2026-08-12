package com.linqibin.blog.post.web;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.linqibin.blog.common.request.RequestIdUtils;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PostImportExportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryPostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository.clear();
    }

    @Test
    void importMarkdownCreatesDraftAndIgnoresPublishedStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "imported-post.md",
                "text/markdown",
                """
                        ---
                        title: Imported Via API
                        slug: imported-via-api
                        status: published
                        ---
                        # Hello Import
                        """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/imports").file(file)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "import-request-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "import-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.title").value("Imported Via API"))
                .andExpect(jsonPath("$.data.slug").value("imported-via-api"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void importRejectsNonMarkdownExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "plain text".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/imports").file(file)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "import-invalid-request-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"))
                .andExpect(jsonPath("$.requestId").value("import-invalid-request-id"));
    }

    @Test
    void exportPostReturnsMarkdownAttachment() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/admin/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Export Via API",
                                  "markdownContent": "# Export Body",
                                  "slug": "export-via-api"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        String postId = response.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/admin/posts/" + postId + "/export")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "export-request-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("export-via-api.md")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/markdown")))
                .andExpect(content().string(containsString("title: Export Via API")))
                .andExpect(content().string(containsString("# Export Body")));
    }

    @Test
    void exportUnknownPostReturns404() throws Exception {
        mockMvc.perform(get("/api/admin/posts/00000000-0000-0000-0000-000000000001/export")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "export-missing-request-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").value("export-missing-request-id"));
    }
}
