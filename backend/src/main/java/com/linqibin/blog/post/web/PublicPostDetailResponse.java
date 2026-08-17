package com.linqibin.blog.post.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.linqibin.blog.markdown.parser.TableOfContentsItem;

// 公开文章详情：返回渲染后的 HTML、目录、摘要、阅读时长和 SEO 元信息。
// 前端用此数据渲染文章页面和生成 SSR 的 meta 标签。
public record PublicPostDetailResponse(
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
        // SEO 字段：前端用于生成 title、description、canonical、Open Graph 标签。
        String seoTitle,
        String seoDescription,
        String canonicalUrl
) {
}
