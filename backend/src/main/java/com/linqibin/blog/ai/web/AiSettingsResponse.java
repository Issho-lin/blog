package com.linqibin.blog.ai.web;

import java.time.Instant;

import com.linqibin.blog.ai.domain.AiSettings;

public record AiSettingsResponse(
        boolean enabled,
        boolean assistantEnabled,
        String chatBaseUrl,
        boolean chatApiKeyConfigured,
        String chatModel,
        String embedBaseUrl,
        boolean embedApiKeyConfigured,
        String embedModel,
        int embedDimensions,
        String assistantPersona,
        int ratePerMinute,
        int ratePerDay,
        Instant updatedAt
) {

    public static AiSettingsResponse from(AiSettings settings) {
        return new AiSettingsResponse(
                settings.enabled(),
                settings.assistantEnabled(),
                settings.chatBaseUrl(),
                !settings.chatApiKey().isBlank(),
                settings.chatModel(),
                settings.embedBaseUrl(),
                !settings.embedApiKey().isBlank(),
                settings.embedModel(),
                settings.embedDimensions(),
                settings.assistantPersona(),
                settings.ratePerMinute(),
                settings.ratePerDay(),
                settings.updatedAt()
        );
    }
}
