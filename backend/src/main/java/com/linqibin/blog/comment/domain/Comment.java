package com.linqibin.blog.comment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Comment(
        UUID id,
        UUID postId,
        String authorName,
        String content,
        String ip,
        Instant createdAt
) {

    public static final int MAX_AUTHOR_NAME_LENGTH = 40;
    public static final int MAX_CONTENT_LENGTH = 2000;

    public Comment {
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(postId, "文章 id 不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        authorName = normalizeAuthorName(authorName);
        content = normalizeContent(content);
        ip = ip == null || ip.isBlank() ? null : ip.trim();
    }

    public static Comment create(
            UUID id,
            UUID postId,
            String authorName,
            String content,
            String ip,
            Instant now
    ) {
        return new Comment(id, postId, authorName, content, ip, now);
    }

    private static String normalizeAuthorName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("称呼不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_AUTHOR_NAME_LENGTH) {
            throw new IllegalArgumentException("称呼不能超过 " + MAX_AUTHOR_NAME_LENGTH + " 个字符");
        }
        return trimmed;
    }

    private static String normalizeContent(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("评论不能超过 " + MAX_CONTENT_LENGTH + " 个字符");
        }
        return trimmed;
    }
}
