package com.linqibin.blog.markdown.parser;

import java.util.List;

// Markdown 渲染器接口：将 Markdown 原文解析为 HTML，并提取目录结构。
// 抽象出来是为了后续可以替换底层解析库，上层逻辑不受影响。
public interface MarkdownRenderer {

    // 将 Markdown 原文渲染为 HTML 字符串。
    // 渲染后的 HTML 中标题（h1-h6）会带有 id 属性，用于锚点跳转。
    // 传入 null 或空白时返回空字符串，避免下游处理空指针。
    String renderToHtml(String markdown);

    // 从 Markdown 原文中提取标题层级结构，生成目录数据。
    // 只提取 H2 和 H3 级别的标题，对应前端侧边目录的常见设计。
    // 返回的锚点与 renderToHtml 中标题的 id 属性保持一致。
    // 传入 null 或空白时返回空列表。
    List<TableOfContentsItem> extractTableOfContents(String markdown);
}
