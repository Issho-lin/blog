package com.linqibin.blog.taxonomy.web;

import jakarta.validation.constraints.NotBlank;

// 创建分类请求对象：承接管理端传入的分类名称、可选 slug 和描述。
public record CreateCategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        String name,
        // slug 可以不传，不传时由系统根据名称自动生成。
        String slug,
        // 描述是可选的。
        String description
) {
}
