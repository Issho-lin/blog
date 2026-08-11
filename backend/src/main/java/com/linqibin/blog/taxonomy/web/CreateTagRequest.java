package com.linqibin.blog.taxonomy.web;

import jakarta.validation.constraints.NotBlank;

// 创建标签请求对象：承接管理端传入的标签名称和可选 slug。
public record CreateTagRequest(
        @NotBlank(message = "标签名称不能为空")
        String name,
        // slug 可以不传，不传时由系统根据名称自动生成。
        String slug
) {
}
