package com.linqibin.blog.markdown.web;

// Markdown 预览响应对象：返回渲染并清洗后的安全 HTML。
public record PreviewResponse(
        String html
) {

    public static PreviewResponse of(String html) {
        return new PreviewResponse(html);
    }
}
