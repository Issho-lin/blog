package com.linqibin.blog.post.web;

import jakarta.validation.constraints.NotBlank;

// 创建草稿请求对象：承接管理端传入的标题、正文和可选 slug。
public record CreatePostRequest(
        // 标题是创建文章的最小必填项。
        @NotBlank(message = "标题不能为空")
        String title,
        // 正文允许先为空，后续再补充。
        String markdownContent,
        // slug 可以不传，不传时由系统根据标题自动生成。
        String slug
) {
}
