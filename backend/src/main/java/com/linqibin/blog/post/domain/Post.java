package com.linqibin.blog.post.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.linqibin.blog.post.exception.InvalidPostStateTransitionException;

public record Post(
        UUID id,
        String title,
        String slug,
        String markdownContent,
        PostStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        PostStatus previousStatusBeforeTrash
) {

    public Post {
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(title, "标题不能为空");
        Objects.requireNonNull(slug, "slug 不能为空");
        Objects.requireNonNull(markdownContent, "正文不能为空");
        Objects.requireNonNull(status, "状态不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        Objects.requireNonNull(updatedAt, "更新时间不能为空");

        title = title.trim();
        slug = slug.trim();

        if (title.isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug 不能为空");
        }
    }

    public static Post createDraft(
            UUID id,
            String title,
            String slug,
            String markdownContent,
            Instant now
    ) {
        return new Post(id, title, slug, markdownContent, PostStatus.DRAFT, now, now, null, null);
    }

    public Post publish(Instant now) {
        if (status != PostStatus.DRAFT && status != PostStatus.PUBLISHED && status != PostStatus.UNPUBLISHED) {
            throw new InvalidPostStateTransitionException(status, "publish");
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (markdownContent.isBlank()) {
            throw new IllegalArgumentException("正文不能为空");
        }

        Instant firstPublishedAt = publishedAt == null ? now : publishedAt;
        return new Post(id, title, slug, markdownContent, PostStatus.PUBLISHED, createdAt, now, firstPublishedAt, null);
    }

    public Post unpublish(Instant now) {
        if (status != PostStatus.PUBLISHED) {
            throw new InvalidPostStateTransitionException(status, "unpublish");
        }
        return new Post(id, title, slug, markdownContent, PostStatus.UNPUBLISHED, createdAt, now, publishedAt, null);
    }

    public Post moveToTrash(Instant now) {
        if (status != PostStatus.DRAFT && status != PostStatus.UNPUBLISHED) {
            throw new InvalidPostStateTransitionException(status, "moveToTrash");
        }
        return new Post(id, title, slug, markdownContent, PostStatus.TRASHED, createdAt, now, publishedAt, status);
    }

    public Post restoreFromTrash(Instant now) {
        if (status != PostStatus.TRASHED) {
            throw new InvalidPostStateTransitionException(status, "restoreFromTrash");
        }
        PostStatus restoredStatus = previousStatusBeforeTrash == PostStatus.UNPUBLISHED
                ? PostStatus.UNPUBLISHED
                : PostStatus.DRAFT;
        return new Post(id, title, slug, markdownContent, restoredStatus, createdAt, now, publishedAt, null);
    }
}
