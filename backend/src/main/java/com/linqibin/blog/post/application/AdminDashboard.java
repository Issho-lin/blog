package com.linqibin.blog.post.application;

import java.util.List;

import com.linqibin.blog.post.domain.Post;

// 管理控制台聚合数据：计数、最近编辑和最近发布。
public record AdminDashboard(
        long total,
        long published,
        long draft,
        long unpublished,
        long trashed,
        long publishedViewCount,
        List<Post> recentlyEdited,
        List<Post> recentlyPublished
) {
}
