package com.linqibin.blog.post.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.linqibin.blog.post.exception.InvalidPostStateTransitionException;

// 文章领域实体：只关心文章自身的数据和状态流转规则。
public record Post(
        UUID id,
        String title,
        String slug,
        String markdownContent,
        PostStatus status,
        UUID categoryId,
        List<UUID> tagIds,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        PostStatus previousStatusBeforeTrash,
        long version,
        long viewCount
) {

    public Post {
        // 记录对象创建时先做基础兜底校验，避免出现不完整的文章实体。
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(title, "标题不能为空");
        Objects.requireNonNull(slug, "slug 不能为空");
        Objects.requireNonNull(markdownContent, "正文不能为空");
        Objects.requireNonNull(status, "状态不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        Objects.requireNonNull(updatedAt, "更新时间不能为空");

        // tagIds 为 null 时默认空列表，避免后续空指针。
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);

        title = title.trim();
        slug = slug.trim();

        // 标题和 slug 允许传入带空格的原始值，但最终落到实体里时不能为空。
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
            UUID categoryId,
            List<UUID> tagIds,
            Instant now
    ) {
        // 新建文章时默认就是草稿，发布时间和回收站前状态都还不存在，version 和 viewCount 从 0 开始。
        return new Post(id, title, slug, markdownContent, PostStatus.DRAFT,
                categoryId, tagIds, now, now, null, null, 0, 0);
    }

    // 更新文章内容、分类和标签。每次修改都递增 version，用于乐观锁并发冲突检测。
    public Post update(String title, String slug, String markdownContent,
                       UUID categoryId, List<UUID> tagIds, Instant now) {
        // 回收站里的文章必须先恢复，再允许继续编辑，避免"已删除内容"被悄悄改动。
        if (status == PostStatus.TRASHED) {
            throw new InvalidPostStateTransitionException(status, "update");
        }
        // 已发布文章更新后仍会对外可见，所以标题和正文依然不能为空。
        if (status == PostStatus.PUBLISHED && markdownContent.isBlank()) {
            throw new IllegalArgumentException("正文不能为空");
        }

        return new Post(id, title, slug, markdownContent, status,
                categoryId, tagIds, createdAt, now, publishedAt, previousStatusBeforeTrash,
                version + 1, viewCount);
    }

    public Post publish(Instant now) {
        // 只有草稿、已发布、已下线的文章才允许发布；回收站里的文章必须先恢复。
        if (status != PostStatus.DRAFT && status != PostStatus.PUBLISHED && status != PostStatus.UNPUBLISHED) {
            throw new InvalidPostStateTransitionException(status, "publish");
        }
        // 发布前必须补齐标题和正文，避免公开文章为空壳。
        if (title.isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (markdownContent.isBlank()) {
            throw new IllegalArgumentException("正文不能为空");
        }

        // 重复发布时保留首次发布时间，只更新最近一次操作时间。阅读数保持不变。
        Instant firstPublishedAt = publishedAt == null ? now : publishedAt;
        return new Post(id, title, slug, markdownContent, PostStatus.PUBLISHED,
                categoryId, tagIds, createdAt, now, firstPublishedAt, null,
                version + 1, viewCount);
    }

    public Post unpublish(Instant now) {
        // 只有当前已经发布的文章才谈得上下线。
        if (status != PostStatus.PUBLISHED) {
            throw new InvalidPostStateTransitionException(status, "unpublish");
        }
        return new Post(id, title, slug, markdownContent, PostStatus.UNPUBLISHED,
                categoryId, tagIds, createdAt, now, publishedAt, null,
                version + 1, viewCount);
    }

    public Post moveToTrash(Instant now) {
        // 这里不允许直接把已发布文章丢进回收站，要求先下线，避免公开状态和回收站状态混在一起。
        if (status != PostStatus.DRAFT && status != PostStatus.UNPUBLISHED) {
            throw new InvalidPostStateTransitionException(status, "moveToTrash");
        }
        return new Post(id, title, slug, markdownContent, PostStatus.TRASHED,
                categoryId, tagIds, createdAt, now, publishedAt, status,
                version + 1, viewCount);
    }

    public Post restoreFromTrash(Instant now) {
        // 恢复时根据回收站前的状态回到草稿或已下线，而不是盲目恢复成某个固定状态。
        if (status != PostStatus.TRASHED) {
            throw new InvalidPostStateTransitionException(status, "restoreFromTrash");
        }
        PostStatus restoredStatus = previousStatusBeforeTrash == PostStatus.UNPUBLISHED
                ? PostStatus.UNPUBLISHED
                : PostStatus.DRAFT;
        return new Post(id, title, slug, markdownContent, restoredStatus,
                categoryId, tagIds, createdAt, now, publishedAt, null,
                version + 1, viewCount);
    }

    public void assertPermanentlyDeletable() {
        // 彻底删除不可恢复，只允许对已经在回收站里的文章执行。
        if (status != PostStatus.TRASHED) {
            throw new InvalidPostStateTransitionException(status, "permanentlyDelete");
        }
    }

    // 文章被公开访问时递增阅读数。不递增 version，因为这不是作者编辑操作。
    public Post incrementViewCount() {
        return new Post(id, title, slug, markdownContent, status,
                categoryId, tagIds, createdAt, updatedAt, publishedAt,
                previousStatusBeforeTrash, version, viewCount + 1);
    }
}
