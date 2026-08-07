package com.linqibin.blog.post.web;

import java.time.Instant;
import java.util.UUID;

import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostStatus;

// 给前端返回的文章视图对象，避免直接暴露领域对象本身。
public record PostResponse(
        UUID id,
        String title,
        String slug,
        String markdownContent,
        PostStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        long version
) {

    public static PostResponse from(Post post) {
        // 统一在这一层做领域对象 -> 接口响应对象的转换。
        // 这样 web 层对外暴露的数据结构可以独立演进，不会把领域对象直接绑死到接口上。
        return new PostResponse(
                post.id(),
                post.title(),
                post.slug(),
                post.markdownContent(),
                post.status(),
                post.createdAt(),
                post.updatedAt(),
                post.publishedAt(),
                post.version()
        );
    }
}
