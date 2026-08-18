package com.linqibin.blog.post.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.linqibin.blog.markdown.parser.TableOfContentsItem;
import com.linqibin.blog.post.domain.PostStatus;

public record AdminPostPreviewResponse(
        UUID id,
        String title,
        String slug,
        String html,
        String markdownContent,
        String summary,
        String coverUrl,
        List<TableOfContentsItem> tableOfContents,
        int readingTimeMinutes,
        long viewCount,
        Instant publishedAt,
        Instant updatedAt,
        String categoryName,
        String categorySlug,
        List<String> tagNames,
        List<String> tagSlugs,
        String seoTitle,
        String seoDescription,
        String canonicalUrl,
        PublicPostNeighbor previousPost,
        PublicPostNeighbor nextPost,
        PostStatus status
) {

    public static AdminPostPreviewResponse from(PostStatus status, PublicPostDetailResponse detail) {
        return new AdminPostPreviewResponse(
                detail.id(),
                detail.title(),
                detail.slug(),
                detail.html(),
                detail.markdownContent(),
                detail.summary(),
                detail.coverUrl(),
                detail.tableOfContents(),
                detail.readingTimeMinutes(),
                detail.viewCount(),
                detail.publishedAt(),
                detail.updatedAt(),
                detail.categoryName(),
                detail.categorySlug(),
                detail.tagNames(),
                detail.tagSlugs(),
                detail.seoTitle(),
                detail.seoDescription(),
                detail.canonicalUrl(),
                detail.previousPost(),
                detail.nextPost(),
                status
        );
    }
}
