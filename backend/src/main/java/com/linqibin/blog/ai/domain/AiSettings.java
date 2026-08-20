package com.linqibin.blog.ai.domain;

import java.time.Instant;
import java.util.Objects;

public record AiSettings(
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

    public static final int SINGLETON_ID = 1;

    public AiSettings {
        Objects.requireNonNull(updatedAt, "更新时间不能为空");
        chatBaseUrl = blankToEmpty(chatBaseUrl);
        chatApiKey = blankToEmpty(chatApiKey);
        chatModel = blankToEmpty(chatModel);
        embedBaseUrl = blankToEmpty(embedBaseUrl);
        embedApiKey = blankToEmpty(embedApiKey);
        embedModel = blankToEmpty(embedModel);
        assistantPersona = assistantPersona == null ? "" : assistantPersona;
        if (embedDimensions < 8 || embedDimensions > 4096) {
            throw new IllegalArgumentException("向量维度需在 8 到 4096 之间");
        }
        if (ratePerMinute < 1 || ratePerMinute > 120) {
            throw new IllegalArgumentException("每分钟次数需在 1 到 120 之间");
        }
        if (ratePerDay < 1 || ratePerDay > 2000) {
            throw new IllegalArgumentException("每天次数需在 1 到 2000 之间");
        }
        if (assistantPersona.length() > 2000) {
            throw new IllegalArgumentException("助手人设不能超过 2000 个字符");
        }
        if (chatModel.length() > 200) {
            throw new IllegalArgumentException("对话模型名过长");
        }
        if (embedModel.length() > 200) {
            throw new IllegalArgumentException("向量模型名过长");
        }
    }

    public static AiSettings defaults(Instant now) {
        return new AiSettings(
                false,
                false,
                "",
                "",
                "",
                "",
                "",
                "",
                1536,
                "",
                10,
                50,
                now
        );
    }

    public boolean hasChatCredentials() {
        return !chatApiKey.isBlank() && !chatModel.isBlank();
    }

    public String resolvedEmbedBaseUrl() {
        return embedBaseUrl.isBlank() ? chatBaseUrl : embedBaseUrl;
    }

    public String resolvedEmbedApiKey() {
        return embedApiKey.isBlank() ? chatApiKey : embedApiKey;
    }

    public boolean hasEmbedCredentials() {
        return !embedModel.isBlank() && !resolvedEmbedApiKey().isBlank();
    }

    public boolean publicAssistantAvailable() {
        return enabled && assistantEnabled;
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
