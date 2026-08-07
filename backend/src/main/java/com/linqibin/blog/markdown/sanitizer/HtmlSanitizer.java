package com.linqibin.blog.markdown.sanitizer;

// HTML 清洗器接口：过滤 HTML 中的 XSS 威胁，只保留安全标签和属性。
// Markdown 渲染后的 HTML 可能包含用户嵌入的原始 HTML，必须经过清洗才能输出到前端。
public interface HtmlSanitizer {

    // 对输入 HTML 进行清洗，返回只包含安全标签和属性的 HTML。
    String sanitize(String html);
}
