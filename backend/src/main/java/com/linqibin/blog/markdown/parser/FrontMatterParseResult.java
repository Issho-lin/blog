package com.linqibin.blog.markdown.parser;

// Front Matter 解析结果：包含元数据和剩余正文。
public record FrontMatterParseResult(
        FrontMatter frontMatter,
        String body,
        boolean hasFrontMatter
) {

    public static FrontMatterParseResult withoutFrontMatter(String body) {
        return new FrontMatterParseResult(FrontMatter.fromMap(null), body, false);
    }
}
