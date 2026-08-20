package com.linqibin.blog.ai.infrastructure.persistence;

import java.time.Instant;

import com.linqibin.blog.ai.domain.AiSettings;
import com.linqibin.blog.ai.domain.AiSettingsRepository;

public class AiSettingsRepositoryAdapter implements AiSettingsRepository {

    private final SpringDataAiSettingsRepository springDataAiSettingsRepository;
    private final AiSettingsEntityMapper mapper;

    public AiSettingsRepositoryAdapter(
            SpringDataAiSettingsRepository springDataAiSettingsRepository,
            AiSettingsEntityMapper mapper
    ) {
        this.springDataAiSettingsRepository = springDataAiSettingsRepository;
        this.mapper = mapper;
    }

    @Override
    public AiSettings get() {
        return springDataAiSettingsRepository.findById((short) AiSettings.SINGLETON_ID)
                .map(mapper::toDomain)
                .orElseGet(() -> AiSettings.defaults(Instant.parse("2026-01-01T00:00:00Z")));
    }

    @Override
    public AiSettings save(AiSettings settings) {
        return mapper.toDomain(springDataAiSettingsRepository.save(mapper.toEntity(settings)));
    }
}
