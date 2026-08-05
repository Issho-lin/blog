package com.linqibin.blog.post.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data JPA 仓库：直接面向 PostEntity 提供查表能力。
public interface SpringDataPostRepository extends JpaRepository<PostEntity, UUID> {

    // 前台按 slug 读取文章时，底层最终会走到这个查询方法。
    Optional<PostEntity> findBySlug(String slug);

    // 创建或编辑文章时，用它判断 slug 是否已被其他记录占用。
    boolean existsBySlug(String slug);
}
