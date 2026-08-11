package com.linqibin.blog.markdown;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.linqibin.blog.markdown.exporter.FrontMatterExporter;
import com.linqibin.blog.markdown.parser.CommonMarkRenderer;
import com.linqibin.blog.markdown.parser.FrontMatterParser;
import com.linqibin.blog.markdown.parser.MarkdownRenderer;
import com.linqibin.blog.markdown.sanitizer.HtmlSanitizer;
import com.linqibin.blog.markdown.sanitizer.OwaspHtmlSanitizer;

// 统一注册 markdown 模块需要的 Spring Bean。
// 解析器和清洗器都只存在一种实现，后续如果需要切换底层库，改这里即可。
@Configuration
public class MarkdownModuleConfiguration {

    @Bean
    public MarkdownRenderer markdownRenderer() {
        // 使用 commonmark-java 作为 Markdown 解析引擎。
        return new CommonMarkRenderer();
    }

    @Bean
    public HtmlSanitizer htmlSanitizer() {
        // 使用 OWASP Java HTML Sanitizer 进行 HTML 清洗。
        return new OwaspHtmlSanitizer();
    }

    @Bean
    public MarkdownService markdownService(MarkdownRenderer markdownRenderer, HtmlSanitizer htmlSanitizer) {
        // 组合解析和清洗，对外暴露统一入口。
        return new MarkdownService(markdownRenderer, htmlSanitizer);
    }

    @Bean
    public FrontMatterParser frontMatterParser() {
        // 用于导入 Markdown 文件时解析 YAML Front Matter。
        return new FrontMatterParser();
    }

    @Bean
    public FrontMatterExporter frontMatterExporter() {
        // 用于导出文章时生成带 Front Matter 的 Markdown 文件。
        return new FrontMatterExporter();
    }
}
