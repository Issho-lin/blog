package com.linqibin.blog.ai.infrastructure;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.linqibin.blog.ai.application.AiSettingsService;
import com.linqibin.blog.ai.domain.AiSettingsRepository;
import com.linqibin.blog.ai.infrastructure.persistence.AiSettingsEntityMapper;
import com.linqibin.blog.ai.infrastructure.persistence.AiSettingsRepositoryAdapter;
import com.linqibin.blog.ai.infrastructure.persistence.SpringDataAiSettingsRepository;

@Configuration
public class AiModuleConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "blog.ai",
            name = "repository-type",
            havingValue = "in-memory",
            matchIfMissing = true
    )
    public InMemoryAiSettingsRepository inMemoryAiSettingsRepository() {
        return new InMemoryAiSettingsRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.ai", name = "repository-type", havingValue = "jpa")
    public AiSettingsEntityMapper aiSettingsEntityMapper() {
        return new AiSettingsEntityMapper();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.ai", name = "repository-type", havingValue = "jpa")
    public AiSettingsRepository jpaAiSettingsRepositoryAdapter(
            SpringDataAiSettingsRepository springDataAiSettingsRepository,
            AiSettingsEntityMapper aiSettingsEntityMapper
    ) {
        return new AiSettingsRepositoryAdapter(springDataAiSettingsRepository, aiSettingsEntityMapper);
    }

    @Bean
    public AiSettingsService aiSettingsService(AiSettingsRepository aiSettingsRepository, Clock clock) {
        return new AiSettingsService(aiSettingsRepository, clock);
    }
}
