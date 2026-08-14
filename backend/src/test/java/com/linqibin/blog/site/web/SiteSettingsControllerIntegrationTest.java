package com.linqibin.blog.site.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.linqibin.blog.common.request.RequestIdUtils;
import com.linqibin.blog.site.infrastructure.InMemorySiteSettingsRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SiteSettingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemorySiteSettingsRepository siteSettingsRepository;

    @BeforeEach
    void setUp() {
        siteSettingsRepository.reset();
    }

    @Test
    void publicEndpointReturnsDefaultSettings() throws Exception {
        mockMvc.perform(get("/api/public/site")
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "public-site-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.siteName").value("Linqibin Blog"))
                .andExpect(jsonPath("$.data.postsPerPage").value(20))
                .andExpect(jsonPath("$.data.aboutHtml").isNotEmpty());
    }

    @Test
    void adminUpdateChangesPublicSettings() throws Exception {
        mockMvc.perform(put("/api/admin/site")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "更新后的站点",
                                  "siteSubtitle": "新副标题",
                                  "siteDescription": "新简介",
                                  "authorName": "林",
                                  "authorAvatarUrl": "",
                                  "aboutMarkdown": "## 你好",
                                  "postsPerPage": 8,
                                  "timezone": "Asia/Shanghai",
                                  "defaultLanguage": "zh-CN",
                                  "faviconUrl": "",
                                  "defaultShareImageUrl": ""
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "update-site-request-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteName").value("更新后的站点"))
                .andExpect(jsonPath("$.data.postsPerPage").value(8));

        mockMvc.perform(get("/api/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteName").value("更新后的站点"))
                .andExpect(jsonPath("$.data.aboutHtml").value(org.hamcrest.Matchers.containsString("你好")));
    }

    @Test
    void adminUpdateRejectsBlankName() throws Exception {
        mockMvc.perform(put("/api/admin/site")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "siteName": "",
                                  "siteSubtitle": "",
                                  "siteDescription": "",
                                  "authorName": "",
                                  "authorAvatarUrl": "",
                                  "aboutMarkdown": "",
                                  "postsPerPage": 10,
                                  "timezone": "Asia/Shanghai",
                                  "defaultLanguage": "zh-CN",
                                  "faviconUrl": "",
                                  "defaultShareImageUrl": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
