package com.linqibin.blog.ai.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import com.linqibin.blog.ai.domain.AiSettings;
import com.linqibin.blog.ai.domain.AiSettingsRepository;

public class AiSettingsService {

    private final AiSettingsRepository repository;
    private final Clock clock;

    public AiSettingsService(AiSettingsRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public AiSettings get() {
        return repository.get();
    }

    public AiSettings update(
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
            int ratePerDay
    ) {
        AiSettings current = repository.get();
        Instant now = Instant.now(clock);
        AiSettings next = new AiSettings(
                enabled,
                assistantEnabled,
                chatBaseUrl,
                keepSecret(current.chatApiKey(), chatApiKey),
                chatModel,
                embedBaseUrl,
                keepSecret(current.embedApiKey(), embedApiKey),
                embedModel,
                embedDimensions,
                assistantPersona,
                ratePerMinute,
                ratePerDay,
                now
        );
        return repository.save(next);
    }

    private static String keepSecret(String stored, String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return stored;
        }
        return incoming;
    }
}
