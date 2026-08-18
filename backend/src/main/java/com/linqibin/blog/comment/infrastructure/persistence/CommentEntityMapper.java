package com.linqibin.blog.comment.infrastructure.persistence;

import com.linqibin.blog.comment.domain.Comment;

public class CommentEntityMapper {

    public CommentEntity toEntity(Comment comment) {
        return new CommentEntity(
                comment.id(),
                comment.postId(),
                comment.authorName(),
                comment.content(),
                comment.ip(),
                comment.createdAt()
        );
    }

    public Comment toDomain(CommentEntity entity) {
        return new Comment(
                entity.getId(),
                entity.getPostId(),
                entity.getAuthorName(),
                entity.getContent(),
                entity.getIp(),
                entity.getCreatedAt()
        );
    }
}
