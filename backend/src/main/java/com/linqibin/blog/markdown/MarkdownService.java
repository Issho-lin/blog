package com.linqibin.blog.markdown;

import java.util.Objects;

import com.linqibin.blog.markdown.parser.MarkdownRenderer;
import com.linqibin.blog.markdown.sanitizer.HtmlSanitizer;

// Markdown 服务：组合解析和清洗两步，对外提供"Markdown -> 安全 HTML"的一站式调用。
// 上层（如预览接口或文章详情渲染）只需要依赖这个服务，不需要关心底层用的是什么解析库和清洗库。
public class MarkdownService {

    private final MarkdownRenderer markdownRenderer;
    private final HtmlSanitizer htmlSanitizer;

    public MarkdownService(MarkdownRenderer markdownRenderer, HtmlSanitizer htmlSanitizer) {
        this.markdownRenderer = Objects.requireNonNull(markdownRenderer);
        this.htmlSanitizer = Objects.requireNonNull(htmlSanitizer);
    }

    // 将 Markdown 原文渲染为经过安全清洗的 HTML。
    // 流程：Markdown -> 原始 HTML -> 清洗后的安全 HTML。
    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String rawHtml = markdownRenderer.renderToHtml(markdown);
        return htmlSanitizer.sanitize(rawHtml);
    }
}
