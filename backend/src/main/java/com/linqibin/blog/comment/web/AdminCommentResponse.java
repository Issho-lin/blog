package com.linqibin.blog.comment.web;

import java.util.UUID;

import com.linqibin.blog.comment.domain.Comment;
import com.linqibin.blog.post.domain.Post;

public record AdminCommentResponse(
        UUID id,
        UUID postId,
        String postTitle,
        String postSlug,
        String authorName,
        String content,
        java.time.Instant createdAt
) {

    public static AdminCommentResponse from(Comment comment, Post post) {
        return new AdminCommentResponse(
                comment.id(),
                comment.postId(),
                post.title(),
                post.slug(),
                comment.authorName(),
                comment.content(),
                comment.createdAt()
        );
    }
}
