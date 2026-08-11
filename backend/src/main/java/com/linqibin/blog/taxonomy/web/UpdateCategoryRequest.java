package com.linqibin.blog.taxonomy.web;

import jakarta.validation.constraints.NotBlank;

// 更新分类请求对象：修改分类名称和描述，slug 通过单独接口修改。
public record UpdateCategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        String name,
        String description
) {
}
