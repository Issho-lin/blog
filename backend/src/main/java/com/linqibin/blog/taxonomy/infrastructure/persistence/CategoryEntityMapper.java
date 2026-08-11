package com.linqibin.blog.taxonomy.infrastructure.persistence;

import com.linqibin.blog.taxonomy.domain.Category;

// 专职负责分类领域对象与持久化实体之间的双向翻译。
public class CategoryEntityMapper {

    public CategoryEntity toEntity(Category category) {
        return new CategoryEntity(
                category.id(),
                category.name(),
                category.slug(),
                category.description(),
                category.createdAt(),
                category.updatedAt()
        );
    }

    public Category toDomain(CategoryEntity entity) {
        return new Category(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
