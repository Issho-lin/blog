package com.linqibin.blog.taxonomy.web;

import java.time.Instant;
import java.util.UUID;

import com.linqibin.blog.taxonomy.domain.Category;

// 给前端返回的分类视图对象，避免直接暴露领域对象本身。
public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.id(),
                category.name(),
                category.slug(),
                category.description(),
                category.createdAt(),
                category.updatedAt()
        );
    }
}
