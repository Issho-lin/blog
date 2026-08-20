package com.linqibin.blog.ai.infrastructure;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import com.linqibin.blog.ai.domain.AiSettings;
import com.linqibin.blog.ai.domain.AiSettingsRepository;

public class InMemoryAiSettingsRepository implements AiSettingsRepository {

    private final AtomicReference<AiSettings> settings =
            new AtomicReference<>(AiSettings.defaults(Instant.parse("2026-01-01T00:00:00Z")));

    public void reset() {
        settings.set(AiSettings.defaults(Instant.parse("2026-01-01T00:00:00Z")));
    }

    @Override
    public AiSettings get() {
        return settings.get();
    }

    @Override
    public AiSettings save(AiSettings next) {
        settings.set(next);
        return next;
    }
}
