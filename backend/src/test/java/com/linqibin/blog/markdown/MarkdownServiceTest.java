package com.linqibin.blog.markdown;

import org.junit.jupiter.api.Test;

import com.linqibin.blog.markdown.parser.CommonMarkRenderer;
import com.linqibin.blog.markdown.sanitizer.OwaspHtmlSanitizer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownServiceTest {

    private final MarkdownService markdownService = new MarkdownService(
            new CommonMarkRenderer(),
            new OwaspHtmlSanitizer()
    );

    @Test
    void rendersMarkdownToSafeHtml() {
        String markdown = "# Title\n\nThis is a **bold** paragraph.";
        String html = markdownService.render(markdown);

        assertTrue(html.contains("<h1>Title</h1>"));
        assertTrue(html.contains("<strong>bold</strong>"));
    }

    @Test
    void stripsScriptTagEmbeddedInMarkdownHtmlBlock() {
        // commonmark 会把 HTML 块原样输出，清洗器需要移除其中的 script 标签。
        String markdown = "<script>alert('xss')</script>\n\n# Safe Heading";
        String html = markdownService.render(markdown);

        assertFalse(html.contains("<script"));
        assertFalse(html.contains("alert"));
        assertTrue(html.contains("Safe Heading"));
    }

    @Test
    void stripsJavascriptProtocolInMarkdownLink() {
        String markdown = "[evil](javascript:steal())";
        String html = markdownService.render(markdown);

        assertFalse(html.contains("javascript:"));
        assertFalse(html.contains("steal"));
    }

    @Test
    void stripsOnclickInMarkdownHtmlBlock() {
        String markdown = "<p onclick=\"steal()\">text</p>";
        String html = markdownService.render(markdown);

        assertFalse(html.contains("onclick"));
        assertFalse(html.contains("steal"));
    }

    @Test
    void rendersGfmTableAndSanitizesOutput() {
        String markdown = """
                | Name | Value |
                |------|-------|
                | A    | 1     |
                """;
        String html = markdownService.render(markdown);

        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("Name"));
        assertTrue(html.contains("Value"));
    }

    @Test
    void returnsEmptyForNullInput() {
        assertTrue(markdownService.render(null).isEmpty());
    }

    @Test
    void returnsEmptyForBlankInput() {
        assertTrue(markdownService.render("   ").isEmpty());
    }
}
