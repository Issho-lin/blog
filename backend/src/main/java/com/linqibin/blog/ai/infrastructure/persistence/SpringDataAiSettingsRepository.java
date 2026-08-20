package com.linqibin.blog.ai.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAiSettingsRepository extends JpaRepository<AiSettingsEntity, Short> {
}
