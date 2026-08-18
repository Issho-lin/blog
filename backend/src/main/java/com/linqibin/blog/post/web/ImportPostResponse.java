package com.linqibin.blog.post.web;

import java.util.List;
import java.util.UUID;

import com.linqibin.blog.post.application.ImportOutcome;
import com.linqibin.blog.post.domain.Post;

// 文章导入响应：返回草稿或覆盖后的未发布文章，以及配图警告。
public record ImportPostResponse(
        UUID id,
        String title,
        String slug,
        String status,
        List<String> warnings
) {

    public static ImportPostResponse from(Post post) {
        return from(new ImportOutcome(post, List.of()));
    }

    public static ImportPostResponse from(ImportOutcome outcome) {
        Post post = outcome.post();
        return new ImportPostResponse(
                post.id(),
                post.title(),
                post.slug(),
                post.status().name(),
                outcome.warnings()
        );
    }
}
