package com.linqibin.blog.markdown.parser;

import java.util.List;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

// 基于 commonmark-java 的 Markdown 渲染实现。
// 支持 CommonMark 规范 + GFM 表格和删除线扩展，覆盖博客常用的排版需求。
public class CommonMarkRenderer implements MarkdownRenderer {

    // Parser 和 HtmlRenderer 都是线程安全的，可以复用同一个实例。
    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    public CommonMarkRenderer() {
        // GFM 扩展需要同时注册到 Parser 和 HtmlRenderer 才能生效。
        List<org.commonmark.Extension> extensions = List.of(
                TablesExtension.create(),
                StrikethroughExtension.create()
        );
        this.parser = Parser.builder()
                .extensions(extensions)
                .build();
        this.htmlRenderer = HtmlRenderer.builder()
                .extensions(extensions)
                .build();
    }

    @Override
    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = parser.parse(markdown);
        return htmlRenderer.render(document);
    }
}
