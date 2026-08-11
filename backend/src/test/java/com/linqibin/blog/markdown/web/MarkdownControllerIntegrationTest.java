package com.linqibin.blog.markdown.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.linqibin.blog.common.request.RequestIdUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MarkdownControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void previewReturnsRenderedHtml() throws Exception {
        mockMvc.perform(post("/api/admin/markdown/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "markdown": "# Hello World"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "preview-request-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "preview-request-id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.requestId").value("preview-request-id"))
                .andExpect(jsonPath("$.data.html").value(org.hamcrest.Matchers.containsString("<h1 id=\"hello-world\">Hello World</h1>")));
    }

    @Test
    void previewStripsScriptTagFromMarkdown() throws Exception {
        mockMvc.perform(post("/api/admin/markdown/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "markdown": "<script>alert('xss')</script>\\n\\n# Safe"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "xss-preview-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.html").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))))
                .andExpect(jsonPath("$.data.html").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("alert"))))
                .andExpect(jsonPath("$.data.html").value(org.hamcrest.Matchers.containsString("<h1 id=\"safe\">Safe</h1>")));
    }

    @Test
    void previewReturnsBadRequestWhenMarkdownIsBlank() throws Exception {
        mockMvc.perform(post("/api/admin/markdown/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "markdown": ""
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "blank-preview-request-id"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(RequestIdUtils.REQUEST_ID_HEADER, "blank-preview-request-id"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.requestId").value("blank-preview-request-id"));
    }

    @Test
    void previewReturnsTableOfContents() throws Exception {
        mockMvc.perform(post("/api/admin/markdown/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "markdown": "# Title\\n\\n## First Section\\n\\n### Sub\\n\\n## Second Section"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "toc-preview-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tableOfContents.length()").value(3))
                .andExpect(jsonPath("$.data.tableOfContents[0].level").value(2))
                .andExpect(jsonPath("$.data.tableOfContents[0].text").value("First Section"))
                .andExpect(jsonPath("$.data.tableOfContents[0].anchor").value("first-section"))
                .andExpect(jsonPath("$.data.tableOfContents[1].level").value(3))
                .andExpect(jsonPath("$.data.tableOfContents[2].text").value("Second Section"));
    }

    @Test
    void previewReturnsEmptyTableOfContentsForNoHeadings() throws Exception {
        mockMvc.perform(post("/api/admin/markdown/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "markdown": "Just a paragraph."
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "no-toc-preview-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tableOfContents").isEmpty());
    }
}
