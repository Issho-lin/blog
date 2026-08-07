package com.linqibin.blog.post.infrastructure.persistence;

import com.linqibin.blog.post.domain.Post;

// 专职负责领域对象与持久化实体之间的双向翻译，避免转换逻辑散落在适配器里。
public class PostEntityMapper {

    public PostEntity toEntity(Post post) {
        return new PostEntity(
                post.id(),
                post.title(),
                post.slug(),
                post.markdownContent(),
                post.status(),
                post.createdAt(),
                post.updatedAt(),
                post.publishedAt(),
                post.previousStatusBeforeTrash(),
                post.version()
        );
    }

    public Post toDomain(PostEntity entity) {
        return new Post(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getMarkdownContent(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPublishedAt(),
                entity.getPreviousStatusBeforeTrash(),
                entity.getVersion()
        );
    }
}
