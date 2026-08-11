package com.linqibin.blog.markdown.parser;

// 目录条目：描述一个标题的层级、文本和锚点，供前端生成可点击的侧边目录。
public record TableOfContentsItem(
        int level,
        String text,
        String anchor
) {
}
