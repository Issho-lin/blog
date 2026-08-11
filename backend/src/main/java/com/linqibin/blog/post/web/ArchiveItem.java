package com.linqibin.blog.post.web;

import java.time.Instant;

// 归档列表项：只包含标题、slug 和发布时间，供归档页面展示。
public record ArchiveItem(
        String title,
        String slug,
        Instant publishedAt
) {
}
