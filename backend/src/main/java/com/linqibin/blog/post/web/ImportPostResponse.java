package com.linqibin.blog.post.web;

import java.util.UUID;

import com.linqibin.blog.post.domain.Post;

// 文章导入响应：返回创建的草稿文章信息。
public record ImportPostResponse(
        UUID id,
        String title,
        String slug,
        String status
) {

    public static ImportPostResponse from(Post post) {
        return new ImportPostResponse(
                post.id(),
                post.title(),
                post.slug(),
                post.status().name()
        );
    }
}
