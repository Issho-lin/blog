package com.linqibin.blog.taxonomy.web;

import java.time.Instant;
import java.util.UUID;

import com.linqibin.blog.taxonomy.domain.Tag;

// 给前端返回的标签视图对象，避免直接暴露领域对象本身。
public record TagResponse(
        UUID id,
        String name,
        String slug,
        Instant createdAt,
        Instant updatedAt
) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(
                tag.id(),
                tag.name(),
                tag.slug(),
                tag.createdAt(),
                tag.updatedAt()
        );
    }
}
