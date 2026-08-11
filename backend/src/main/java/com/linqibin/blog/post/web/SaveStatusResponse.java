package com.linqibin.blog.post.web;

import java.time.Instant;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostStatus;

// 保存状态响应：轻量返回版本号、更新时间和状态，让前端在不拉取全文的情况下确认服务端最新状态。
public record SaveStatusResponse(
        long version,
        Instant updatedAt,
        PostStatus status
) {

    public static SaveStatusResponse from(Post post) {
        return new SaveStatusResponse(post.version(), post.updatedAt(), post.status());
    }
}
