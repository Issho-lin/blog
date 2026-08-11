package com.linqibin.blog.markdown;

import java.util.List;

import com.linqibin.blog.markdown.parser.TableOfContentsItem;

// Markdown 渲染结果：同时返回安全 HTML 和目录数据，供预览接口一次性返回前端。
public record MarkdownRenderResult(
        String html,
        List<TableOfContentsItem> tableOfContents
) {
}
