package com.linqibin.blog.markdown.web;

import java.util.List;

import com.linqibin.blog.markdown.parser.TableOfContentsItem;

// Markdown 预览响应对象：返回渲染并清洗后的安全 HTML，以及从标题中提取的目录数据。
public record PreviewResponse(
        String html,
        List<TableOfContentsItem> tableOfContents
) {

    public static PreviewResponse of(String html, List<TableOfContentsItem> tableOfContents) {
        return new PreviewResponse(html, tableOfContents);
    }
}
