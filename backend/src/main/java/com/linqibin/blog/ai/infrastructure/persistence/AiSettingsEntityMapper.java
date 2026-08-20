package com.linqibin.blog.ai.infrastructure.persistence;

import com.linqibin.blog.ai.domain.AiSettings;

public class AiSettingsEntityMapper {

    public AiSettingsEntity toEntity(AiSettings settings) {
        return new AiSettingsEntity(
                (short) AiSettings.SINGLETON_ID,
                settings.enabled(),
                settings.assistantEnabled(),
                settings.chatBaseUrl(),
                settings.chatApiKey(),
                settings.chatModel(),
                settings.embedBaseUrl(),
                settings.embedApiKey(),
                settings.embedModel(),
                settings.embedDimensions(),
                settings.assistantPersona(),
                settings.ratePerMinute(),
                settings.ratePerDay(),
                settings.updatedAt()
        );
    }

    public AiSettings toDomain(AiSettingsEntity entity) {
        return new AiSettings(
                entity.isEnabled(),
                entity.isAssistantEnabled(),
                entity.getChatBaseUrl(),
                entity.getChatApiKey(),
                entity.getChatModel(),
                entity.getEmbedBaseUrl(),
                entity.getEmbedApiKey(),
                entity.getEmbedModel(),
                entity.getEmbedDimensions(),
                entity.getAssistantPersona(),
                entity.getRatePerMinute(),
                entity.getRatePerDay(),
                entity.getUpdatedAt()
        );
    }
}
