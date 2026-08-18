package com.linqibin.blog.post.web;

import java.time.Instant;
import java.util.UUID;

import com.linqibin.blog.post.domain.PostRevision;
import com.linqibin.blog.post.domain.PostRevisionKind;

public record PostRevisionSummaryResponse(
        UUID id,
        String title,
        PostRevisionKind kind,
        Instant createdAt
) {

    public static PostRevisionSummaryResponse from(PostRevision revision) {
        return new PostRevisionSummaryResponse(
                revision.id(),
                revision.title(),
                revision.kind(),
                revision.createdAt()
        );
    }
}
