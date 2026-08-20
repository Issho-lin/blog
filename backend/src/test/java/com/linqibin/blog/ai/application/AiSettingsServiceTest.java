package com.linqibin.blog.ai.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.linqibin.blog.ai.domain.AiSettings;
import com.linqibin.blog.ai.infrastructure.InMemoryAiSettingsRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSettingsServiceTest {

    @Test
    void blankApiKeyKeepsPreviousSecret() {
        InMemoryAiSettingsRepository repository = new InMemoryAiSettingsRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC);
        AiSettingsService service = new AiSettingsService(repository, clock);

        service.update(true, false, "https://api.deepseek.com", "sk-old", "deepseek-chat",
                "", "", "", 1536, "", 10, 50);
        AiSettings kept = service.update(true, true, "https://api.deepseek.com", "", "deepseek-chat",
                "", "", "", 1536, "人设", 6, 30);

        assertEquals("sk-old", kept.chatApiKey());
        assertTrue(kept.assistantEnabled());
        assertEquals(6, kept.ratePerMinute());
    }

    @Test
    void embedFallsBackToChatCredentialsWhenEmbedKeyBlank() {
        InMemoryAiSettingsRepository repository = new InMemoryAiSettingsRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC);
        AiSettingsService service = new AiSettingsService(repository, clock);

        AiSettings saved = service.update(
                true, true, "https://api.deepseek.com", "sk-chat", "deepseek-chat",
                "", "", "text-embedding-3-small", 1536, "", 10, 50
        );

        assertEquals("sk-chat", saved.resolvedEmbedApiKey());
        assertEquals("https://api.deepseek.com", saved.resolvedEmbedBaseUrl());
        assertTrue(saved.hasEmbedCredentials());
    }
}
