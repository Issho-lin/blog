package com.linqibin.blog.taxonomy.web;

import jakarta.validation.constraints.NotBlank;

// 更新标签请求对象：修改标签名称，slug 通过单独接口修改。
public record UpdateTagRequest(
        @NotBlank(message = "标签名称不能为空")
        String name
) {
}
