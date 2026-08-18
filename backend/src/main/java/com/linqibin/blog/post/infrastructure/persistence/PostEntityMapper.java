package com.linqibin.blog.post.infrastructure.persistence;

import com.linqibin.blog.post.domain.Post;

// 专职负责领域对象与持久化实体之间的双向翻译，避免转换逻辑散落在适配器里。
public class PostEntityMapper {

    public PostEntity toEntity(Post post) {
        return new PostEntity(
                post.id(),
                post.title(),
                post.slug(),
                post.excerpt(),
                post.coverUrl(),
                post.seoTitle(),
                post.seoDescription(),
                post.markdownContent(),
                post.status(),
                post.categoryId(),
                post.tagIds(),
                post.createdAt(),
                post.updatedAt(),
                post.publishedAt(),
                post.previousStatusBeforeTrash(),
                post.version(),
                post.viewCount()
        );
    }

    public Post toDomain(PostEntity entity) {
        return new Post(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getExcerpt(),
                entity.getCoverUrl(),
                entity.getSeoTitle(),
                entity.getSeoDescription(),
                entity.getMarkdownContent(),
                entity.getStatus(),
                entity.getCategoryId(),
                entity.getTagIds(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPublishedAt(),
                entity.getPreviousStatusBeforeTrash(),
                entity.getVersion(),
                entity.getViewCount()
        );
    }
}
