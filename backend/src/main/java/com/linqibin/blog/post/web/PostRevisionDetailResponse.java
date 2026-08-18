package com.linqibin.blog.post.web;

import java.time.Instant;
import java.util.UUID;

import com.linqibin.blog.post.domain.PostRevision;
import com.linqibin.blog.post.domain.PostRevisionKind;

public record PostRevisionDetailResponse(
        UUID id,
        UUID postId,
        String title,
        String markdownContent,
        String excerpt,
        PostRevisionKind kind,
        Instant createdAt
) {

    public static PostRevisionDetailResponse from(PostRevision revision) {
        return new PostRevisionDetailResponse(
                revision.id(),
                revision.postId(),
                revision.title(),
                revision.markdownContent(),
                revision.excerpt(),
                revision.kind(),
                revision.createdAt()
        );
    }
}
