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
        String excerpt,
        String coverUrl,
        String seoTitle,
        String seoDescription,
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

    private static final int MAX_EXCERPT_LENGTH = 500;
    private static final int MAX_COVER_URL_LENGTH = 500;
    private static final int MAX_SEO_TITLE_LENGTH = 120;
    private static final int MAX_SEO_DESCRIPTION_LENGTH = 500;

    public Post {
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(title, "标题不能为空");
        Objects.requireNonNull(slug, "slug 不能为空");
        Objects.requireNonNull(markdownContent, "正文不能为空");
        Objects.requireNonNull(status, "状态不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        Objects.requireNonNull(updatedAt, "更新时间不能为空");

        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);

        title = title.trim();
        slug = slug.trim();
        excerpt = normalizeOptional(excerpt, MAX_EXCERPT_LENGTH, "摘要");
        coverUrl = normalizeOptional(coverUrl, MAX_COVER_URL_LENGTH, "封面地址");
        seoTitle = normalizeOptional(seoTitle, MAX_SEO_TITLE_LENGTH, "SEO 标题");
        seoDescription = normalizeOptional(seoDescription, MAX_SEO_DESCRIPTION_LENGTH, "SEO 描述");

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
        return createDraft(id, title, slug, markdownContent, categoryId, tagIds, now, null, null, null, null);
    }

    public static Post createDraft(
            UUID id,
            String title,
            String slug,
            String markdownContent,
            UUID categoryId,
            List<UUID> tagIds,
            Instant now,
            String excerpt,
            String coverUrl
    ) {
        return createDraft(id, title, slug, markdownContent, categoryId, tagIds, now,
                excerpt, coverUrl, null, null);
    }

    public static Post createDraft(
            UUID id,
            String title,
            String slug,
            String markdownContent,
            UUID categoryId,
            List<UUID> tagIds,
            Instant now,
            String excerpt,
            String coverUrl,
            String seoTitle,
            String seoDescription
    ) {
        return new Post(id, title, slug, excerpt, coverUrl, seoTitle, seoDescription,
                markdownContent, PostStatus.DRAFT, categoryId, tagIds, now, now, null, null, 0, 0);
    }

    public Post update(String title, String slug, String markdownContent,
                       UUID categoryId, List<UUID> tagIds, Instant now,
                       String excerpt, String coverUrl) {
        return update(title, slug, markdownContent, categoryId, tagIds, now,
                excerpt, coverUrl, seoTitle, seoDescription);
    }

    public Post update(String title, String slug, String markdownContent,
                       UUID categoryId, List<UUID> tagIds, Instant now,
                       String excerpt, String coverUrl,
                       String seoTitle, String seoDescription) {
        if (status == PostStatus.TRASHED) {
            throw new InvalidPostStateTransitionException(status, "update");
        }
        if (status == PostStatus.PUBLISHED && markdownContent.isBlank()) {
            throw new IllegalArgumentException("正文不能为空");
        }

        return copy(title, slug, excerpt, coverUrl, seoTitle, seoDescription,
                markdownContent, status, categoryId, tagIds, now, publishedAt,
                previousStatusBeforeTrash, version + 1, viewCount);
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
        return copy(title, slug, excerpt, coverUrl, seoTitle, seoDescription,
                markdownContent, PostStatus.PUBLISHED, categoryId, tagIds, now, firstPublishedAt,
                null, version + 1, viewCount);
    }

    public Post unpublish(Instant now) {
        if (status != PostStatus.PUBLISHED) {
            throw new InvalidPostStateTransitionException(status, "unpublish");
        }
        return copy(title, slug, excerpt, coverUrl, seoTitle, seoDescription,
                markdownContent, PostStatus.UNPUBLISHED, categoryId, tagIds, now, publishedAt,
                null, version + 1, viewCount);
    }

    public Post moveToTrash(Instant now) {
        if (status != PostStatus.DRAFT && status != PostStatus.UNPUBLISHED) {
            throw new InvalidPostStateTransitionException(status, "moveToTrash");
        }
        return copy(title, slug, excerpt, coverUrl, seoTitle, seoDescription,
                markdownContent, PostStatus.TRASHED, categoryId, tagIds, now, publishedAt,
                status, version + 1, viewCount);
    }

    public Post restoreFromTrash(Instant now) {
        if (status != PostStatus.TRASHED) {
            throw new InvalidPostStateTransitionException(status, "restoreFromTrash");
        }
        PostStatus restoredStatus = previousStatusBeforeTrash == PostStatus.UNPUBLISHED
                ? PostStatus.UNPUBLISHED
                : PostStatus.DRAFT;
        return copy(title, slug, excerpt, coverUrl, seoTitle, seoDescription,
                markdownContent, restoredStatus, categoryId, tagIds, now, publishedAt,
                null, version + 1, viewCount);
    }

    public void assertPermanentlyDeletable() {
        if (status != PostStatus.TRASHED) {
            throw new InvalidPostStateTransitionException(status, "permanentlyDelete");
        }
    }

    public Post incrementViewCount() {
        return copy(title, slug, excerpt, coverUrl, seoTitle, seoDescription,
                markdownContent, status, categoryId, tagIds, updatedAt, publishedAt,
                previousStatusBeforeTrash, version, viewCount + 1);
    }

    public boolean isPubliclyReadable() {
        return status == PostStatus.PUBLISHED;
    }

    public boolean matchesKeyword(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        if (normalized.isBlank()) {
            return true;
        }
        if (title.toLowerCase().contains(normalized)) {
            return true;
        }
        if (markdownContent.toLowerCase().contains(normalized)) {
            return true;
        }
        if (excerpt != null && excerpt.toLowerCase().contains(normalized)) {
            return true;
        }
        return seoTitle != null && seoTitle.toLowerCase().contains(normalized)
                || seoDescription != null && seoDescription.toLowerCase().contains(normalized);
    }

    private Post copy(
            String title,
            String slug,
            String excerpt,
            String coverUrl,
            String seoTitle,
            String seoDescription,
            String markdownContent,
            PostStatus status,
            UUID categoryId,
            List<UUID> tagIds,
            Instant updatedAt,
            Instant publishedAt,
            PostStatus previousStatusBeforeTrash,
            long version,
            long viewCount
    ) {
        return new Post(
                id, title, slug, excerpt, coverUrl, seoTitle, seoDescription,
                markdownContent, status, categoryId, tagIds, createdAt, updatedAt, publishedAt,
                previousStatusBeforeTrash, version, viewCount
        );
    }

    private static String normalizeOptional(String value, int maxLength, String fieldName) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return trimmed;
    }
}
