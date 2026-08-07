package com.linqibin.blog.markdown.web;

import jakarta.validation.constraints.NotBlank;

// Markdown 预览请求对象：接收编辑器中的 Markdown 原文。
public record PreviewRequest(
        // Markdown 原文不能为空，否则预览没有意义。
        @NotBlank(message = "Markdown 内容不能为空")
        String markdown
) {
}
