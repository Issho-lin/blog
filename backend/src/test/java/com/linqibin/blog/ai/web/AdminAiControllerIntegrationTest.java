package com.linqibin.blog.ai.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.linqibin.blog.ai.infrastructure.InMemoryAiSettingsRepository;
import com.linqibin.blog.common.request.RequestIdUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminAiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryAiSettingsRepository aiSettingsRepository;

    @BeforeEach
    void setUp() {
        aiSettingsRepository.reset();
    }

    @Test
    void summarizeWhenAiDisabledReturnsUnavailable() throws Exception {
        mockMvc.perform(post("/api/admin/ai/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "markdown": "一篇用于生成摘要的正文"
                                }
                                """)
                        .header(RequestIdUtils.REQUEST_ID_HEADER, "ai-summarize-request-id"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AGENT_UNAVAILABLE"))
                .andExpect(jsonPath("$.requestId").value("ai-summarize-request-id"));
    }

    @Test
    void taxonomyWhenAiDisabledReturnsUnavailable() throws Exception {
        mockMvc.perform(post("/api/admin/ai/taxonomy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "标题",
                                  "markdown": "正文"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AGENT_UNAVAILABLE"));
    }

    @Test
    void updateSettingsMasksApiKey() throws Exception {
        mockMvc.perform(put("/api/admin/ai/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "assistantEnabled": true,
                                  "chatBaseUrl": "https://api.deepseek.com",
                                  "chatApiKey": "sk-secret",
                                  "chatModel": "deepseek-chat",
                                  "embedBaseUrl": "",
                                  "embedApiKey": "",
                                  "embedModel": "",
                                  "embedDimensions": 1536,
                                  "assistantPersona": "语气克制",
                                  "ratePerMinute": 8,
                                  "ratePerDay": 40
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.chatModel").value("deepseek-chat"))
                .andExpect(jsonPath("$.data.chatApiKeyConfigured").value(true))
                .andExpect(jsonPath("$.data.chatApiKey").doesNotExist());

        mockMvc.perform(get("/api/admin/ai/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assistantEnabled").value(true))
                .andExpect(jsonPath("$.data.ratePerMinute").value(8));
    }

    @Test
    void publicAssistantHiddenWhenDisabled() throws Exception {
        mockMvc.perform(get("/api/public/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assistantEnabled").value(false));
    }
}
