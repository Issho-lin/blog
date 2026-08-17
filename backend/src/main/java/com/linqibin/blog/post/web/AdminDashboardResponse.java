package com.linqibin.blog.post.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.linqibin.blog.post.application.AdminDashboard;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostStatus;

// 管理控制台响应：不含正文，只返回首页需要的计数和最近文章。
public record AdminDashboardResponse(
        Counts counts,
        List<Item> recentlyEdited,
        List<Item> recentlyPublished
) {

    public record Counts(
            long total,
            long published,
            long draft,
            long unpublished,
            long trashed,
            long publishedViewCount
    ) {
    }

    public record Item(
            UUID id,
            String title,
            String slug,
            PostStatus status,
            Instant updatedAt,
            Instant publishedAt,
            long viewCount
    ) {

        public static Item from(Post post) {
            return new Item(
                    post.id(),
                    post.title(),
                    post.slug(),
                    post.status(),
                    post.updatedAt(),
                    post.publishedAt(),
                    post.viewCount()
            );
        }
    }

    public static AdminDashboardResponse from(AdminDashboard dashboard) {
        return new AdminDashboardResponse(
                new Counts(
                        dashboard.total(),
                        dashboard.published(),
                        dashboard.draft(),
                        dashboard.unpublished(),
                        dashboard.trashed(),
                        dashboard.publishedViewCount()
                ),
                dashboard.recentlyEdited().stream().map(Item::from).toList(),
                dashboard.recentlyPublished().stream().map(Item::from).toList()
        );
    }
}
