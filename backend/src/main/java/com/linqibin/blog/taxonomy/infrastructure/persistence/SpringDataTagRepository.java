package com.linqibin.blog.taxonomy.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data JPA 仓库：直接面向 TagEntity 提供查表能力。
public interface SpringDataTagRepository extends JpaRepository<TagEntity, UUID> {

    // 按 slug 查询标签，给公开接口使用。
    Optional<TagEntity> findBySlug(String slug);

    // 创建或编辑标签时，判断 slug 是否已被占用。
    boolean existsBySlug(String slug);
}
