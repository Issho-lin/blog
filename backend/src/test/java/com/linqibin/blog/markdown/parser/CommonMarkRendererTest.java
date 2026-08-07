package com.linqibin.blog.markdown.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonMarkRendererTest {

    private final CommonMarkRenderer renderer = new CommonMarkRenderer();

    @Test
    void rendersHeadingAndParagraph() {
        String html = renderer.renderToHtml("# Title\n\nHello world.");

        assertTrue(html.contains("<h1>Title</h1>"));
        assertTrue(html.contains("<p>Hello world.</p>"));
    }

    @Test
    void rendersUnorderedList() {
        String html = renderer.renderToHtml("- item1\n- item2");

        assertTrue(html.contains("<ul>"));
        assertTrue(html.contains("<li>item1</li>"));
        assertTrue(html.contains("<li>item2</li>"));
    }

    @Test
    void rendersOrderedList() {
        String html = renderer.renderToHtml("1. first\n2. second");

        assertTrue(html.contains("<ol>"));
        assertTrue(html.contains("<li>first</li>"));
        assertTrue(html.contains("<li>second</li>"));
    }

    @Test
    void rendersLink() {
        String html = renderer.renderToHtml("[Example](https://example.com)");

        assertTrue(html.contains("<a href=\"https://example.com\">Example</a>"));
    }

    @Test
    void rendersImage() {
        String html = renderer.renderToHtml("![alt text](https://example.com/img.png)");

        assertTrue(html.contains("<img src=\"https://example.com/img.png\" alt=\"alt text\""));
    }

    @Test
    void rendersCodeBlock() {
        String html = renderer.renderToHtml("```\ncode block\n```");

        assertTrue(html.contains("<pre>"));
        assertTrue(html.contains("<code>"));
        assertTrue(html.contains("code block"));
    }

    @Test
    void rendersInlineCode() {
        String html = renderer.renderToHtml("This is `inline code`.");

        assertTrue(html.contains("<code>inline code</code>"));
    }

    @Test
    void rendersBlockquote() {
        String html = renderer.renderToHtml("> quoted text");

        assertTrue(html.contains("<blockquote>"));
        assertTrue(html.contains("quoted text"));
    }

    @Test
    void rendersTable() {
        String markdown = """
                | Header 1 | Header 2 |
                |---------|---------|
                | Cell 1  | Cell 2  |
                """;
        String html = renderer.renderToHtml(markdown);

        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<th>Header 1</th>"));
        assertTrue(html.contains("<td>Cell 1</td>"));
    }

    @Test
    void rendersStrikethrough() {
        String html = renderer.renderToHtml("~~deleted~~");

        assertTrue(html.contains("<del>deleted</del>"));
    }

    @Test
    void returnsEmptyForNullInput() {
        assertEquals("", renderer.renderToHtml(null));
    }

    @Test
    void returnsEmptyForBlankInput() {
        assertEquals("", renderer.renderToHtml("   "));
    }
}
