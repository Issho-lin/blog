package com.linqibin.blog.markdown.parser;

// Markdown 渲染器接口：将 Markdown 原文解析为 HTML。
// 抽象出来是为了后续可以替换底层解析库，上层逻辑不受影响。
public interface MarkdownRenderer {

    // 将 Markdown 原文渲染为 HTML 字符串。
    // 传入 null 或空白时返回空字符串，避免下游处理空指针。
    String renderToHtml(String markdown);
}
