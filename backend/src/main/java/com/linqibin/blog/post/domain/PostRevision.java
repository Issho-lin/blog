package com.linqibin.blog.post.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PostRevision(
        UUID id,
        UUID postId,
        String title,
        String markdownContent,
        String excerpt,
        PostRevisionKind kind,
        Instant createdAt
) {

    public PostRevision {
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(postId, "文章 id 不能为空");
        Objects.requireNonNull(title, "标题不能为空");
        Objects.requireNonNull(markdownContent, "正文不能为空");
        Objects.requireNonNull(kind, "版本类型不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        excerpt = excerpt == null || excerpt.isBlank() ? null : excerpt.trim();
    }

    public static PostRevision snapshot(
            UUID id,
            Post post,
            PostRevisionKind kind,
            Instant now
    ) {
        return new PostRevision(
                id,
                post.id(),
                post.title(),
                post.markdownContent(),
                post.excerpt(),
                kind,
                now
        );
    }

    public boolean sameContent(Post post) {
        String otherExcerpt = post.excerpt() == null || post.excerpt().isBlank() ? null : post.excerpt().trim();
        return title.equals(post.title())
                && markdownContent.equals(post.markdownContent())
                && Objects.equals(excerpt, otherExcerpt);
    }
}
