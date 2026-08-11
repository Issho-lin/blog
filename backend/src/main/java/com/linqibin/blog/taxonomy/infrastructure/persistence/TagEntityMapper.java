package com.linqibin.blog.taxonomy.infrastructure.persistence;

import com.linqibin.blog.taxonomy.domain.Tag;

// 专职负责标签领域对象与持久化实体之间的双向翻译。
public class TagEntityMapper {

    public TagEntity toEntity(Tag tag) {
        return new TagEntity(
                tag.id(),
                tag.name(),
                tag.slug(),
                tag.createdAt(),
                tag.updatedAt()
        );
    }

    public Tag toDomain(TagEntity entity) {
        return new Tag(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
