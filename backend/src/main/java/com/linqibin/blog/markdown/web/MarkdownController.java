package com.linqibin.blog.markdown.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.markdown.MarkdownService;

// Markdown 预览接口：接收编辑器中的 Markdown 原文，返回渲染并清洗后的安全 HTML。
// 这个接口不依赖文章是否存在，纯粹是一个 Markdown -> HTML 的转换服务。
@RestController
@RequestMapping("/api/admin/markdown")
public class MarkdownController {

    private final MarkdownService markdownService;

    public MarkdownController(MarkdownService markdownService) {
        this.markdownService = markdownService;
    }

    @PostMapping("/preview")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        // 将 Markdown 渲染为安全 HTML，供前端编辑器实时预览使用。
        String html = markdownService.render(request.markdown());
        return PreviewResponse.of(html);
    }
}
