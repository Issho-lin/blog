package com.linqibin.blog.ai.infrastructure.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_settings")
public class AiSettingsEntity {

    @Id
    @Column(columnDefinition = "smallint")
    private Short id;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "assistant_enabled", nullable = false)
    private boolean assistantEnabled;

    @Column(name = "chat_base_url", length = 500)
    private String chatBaseUrl;

    @Column(name = "chat_api_key", length = 500)
    private String chatApiKey;

    @Column(name = "chat_model", length = 200)
    private String chatModel;

    @Column(name = "embed_base_url", length = 500)
    private String embedBaseUrl;

    @Column(name = "embed_api_key", length = 500)
    private String embedApiKey;

    @Column(name = "embed_model", length = 200)
    private String embedModel;

    @Column(name = "embed_dimensions", nullable = false)
    private int embedDimensions;

    @Column(name = "assistant_persona", length = 2000)
    private String assistantPersona;

    @Column(name = "rate_per_minute", nullable = false)
    private int ratePerMinute;

    @Column(name = "rate_per_day", nullable = false)
    private int ratePerDay;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiSettingsEntity() {
    }

    public AiSettingsEntity(
            Short id,
            boolean enabled,
            boolean assistantEnabled,
            String chatBaseUrl,
            String chatApiKey,
            String chatModel,
            String embedBaseUrl,
            String embedApiKey,
            String embedModel,
            int embedDimensions,
            String assistantPersona,
            int ratePerMinute,
            int ratePerDay,
            Instant updatedAt
    ) {
        this.id = id;
        this.enabled = enabled;
        this.assistantEnabled = assistantEnabled;
        this.chatBaseUrl = chatBaseUrl;
        this.chatApiKey = chatApiKey;
        this.chatModel = chatModel;
        this.embedBaseUrl = embedBaseUrl;
        this.embedApiKey = embedApiKey;
        this.embedModel = embedModel;
        this.embedDimensions = embedDimensions;
        this.assistantPersona = assistantPersona;
        this.ratePerMinute = ratePerMinute;
        this.ratePerDay = ratePerDay;
        this.updatedAt = updatedAt;
    }

    public Short getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAssistantEnabled() {
        return assistantEnabled;
    }

    public String getChatBaseUrl() {
        return chatBaseUrl;
    }

    public String getChatApiKey() {
        return chatApiKey;
    }

    public String getChatModel() {
        return chatModel;
    }

    public String getEmbedBaseUrl() {
        return embedBaseUrl;
    }

    public String getEmbedApiKey() {
        return embedApiKey;
    }

    public String getEmbedModel() {
        return embedModel;
    }

    public int getEmbedDimensions() {
        return embedDimensions;
    }

    public String getAssistantPersona() {
        return assistantPersona;
    }

    public int getRatePerMinute() {
        return ratePerMinute;
    }

    public int getRatePerDay() {
        return ratePerDay;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
