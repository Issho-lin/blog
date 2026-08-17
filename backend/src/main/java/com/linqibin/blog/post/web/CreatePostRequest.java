package com.linqibin.blog.post.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 创建草稿请求对象：承接管理端传入的标题、正文、可选 slug、分类和标签。
public record CreatePostRequest(
        // 标题是创建文章的最小必填项。
        @NotBlank(message = "标题不能为空")
        String title,
        // 正文允许先为空，后续再补充。
        String markdownContent,
        // slug 可以不传，不传时由系统根据标题自动生成。
        String slug,
        // 分类 ID 可选，不传时文章为未分类。
        UUID categoryId,
        // 标签 ID 列表可选，不传时文章无标签。
        List<UUID> tagIds,
        @Size(max = 500, message = "摘要不能超过 500 个字符")
        String excerpt,
        @Size(max = 500, message = "封面地址不能超过 500 个字符")
        String coverUrl
) {
}
