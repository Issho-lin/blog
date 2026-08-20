package com.linqibin.blog.ai.domain;

public interface AiSettingsRepository {

    AiSettings get();

    AiSettings save(AiSettings settings);
}
