package com.linqibin.blog.post.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// 公开文章列表项：轻量响应，不含正文，只返回首页和列表页需要的信息。
public record PublicPostSummary(
        UUID id,
        String title,
        String slug,
        String summary,
        Instant publishedAt,
        int readingTimeMinutes,
        long viewCount,
        String categoryName,
        String categorySlug,
        List<String> tagNames,
        List<String> tagSlugs
) {
}
