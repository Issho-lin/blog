package com.linqibin.blog.comment.web;

import java.time.Instant;
import java.util.UUID;

import com.linqibin.blog.comment.domain.Comment;

public record CommentResponse(
        UUID id,
        UUID postId,
        String authorName,
        String content,
        Instant createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.id(),
                comment.postId(),
                comment.authorName(),
                comment.content(),
                comment.createdAt()
        );
    }
}
