package com.linqibin.blog.post.web;

import jakarta.validation.constraints.NotBlank;

public record CreatePostRequest(
        @NotBlank(message = "标题不能为空")
        String title,
        String markdownContent,
        String slug
) {
}
