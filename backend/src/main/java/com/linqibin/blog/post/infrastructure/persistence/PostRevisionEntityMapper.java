package com.linqibin.blog.post.infrastructure.persistence;

import com.linqibin.blog.post.domain.PostRevision;

public class PostRevisionEntityMapper {

    public PostRevisionEntity toEntity(PostRevision revision) {
        return new PostRevisionEntity(
                revision.id(),
                revision.postId(),
                revision.title(),
                revision.markdownContent(),
                revision.excerpt(),
                revision.kind(),
                revision.createdAt()
        );
    }

    public PostRevision toDomain(PostRevisionEntity entity) {
        return new PostRevision(
                entity.getId(),
                entity.getPostId(),
                entity.getTitle(),
                entity.getMarkdownContent(),
                entity.getExcerpt(),
                entity.getKind(),
                entity.getCreatedAt()
        );
    }
}
