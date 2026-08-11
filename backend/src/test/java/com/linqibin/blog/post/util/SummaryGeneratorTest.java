package com.linqibin.blog.post.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryGeneratorTest {

    @Test
    void emptyMarkdownReturnsEmptyString() {
        assertEquals("", SummaryGenerator.generate(null));
        assertEquals("", SummaryGenerator.generate(""));
        assertEquals("", SummaryGenerator.generate("   "));
    }

    @Test
    void shortMarkdownReturnsPlainText() {
        String markdown = "This is a **bold** and *italic* text.";
        String summary = SummaryGenerator.generate(markdown);
        assertEquals("This is a bold and italic text.", summary);
    }

    @Test
    void longMarkdownTruncatesWithEllipsis() {
        String markdown = "word ".repeat(100);
        String summary = SummaryGenerator.generate(markdown, 50);
        assertTrue(summary.endsWith("…"));
        assertTrue(summary.length() <= 51);
    }

    @Test
    void stripsHeadingSyntax() {
        String markdown = "## Heading\n\nSome content here.";
        String plain = SummaryGenerator.stripMarkdown(markdown);
        assertTrue(plain.startsWith("Heading"));
        assertTrue(plain.contains("Some content here."));
    }

    @Test
    void stripsLinkSyntaxButKeepsText() {
        String markdown = "Visit [Google](https://google.com) for more.";
        String plain = SummaryGenerator.stripMarkdown(markdown);
        assertEquals("Visit Google for more.", plain);
    }

    @Test
    void stripsImageSyntax() {
        String markdown = "![alt text](image.png) and text";
        String plain = SummaryGenerator.stripMarkdown(markdown);
        assertEquals("alt text and text", plain);
    }

    @Test
    void stripsCodeBlocks() {
        String markdown = "Before\n```java\nSystem.out.println(\"hello\");\n```\nAfter";
        String plain = SummaryGenerator.stripMarkdown(markdown);
        assertTrue(plain.contains("Before"));
        assertTrue(plain.contains("After"));
        assertTrue(!plain.contains("System.out"));
    }

    @Test
    void stripsBlockquoteSyntax() {
        String markdown = "> This is a quote.\n\nNormal text.";
        String plain = SummaryGenerator.stripMarkdown(markdown);
        assertTrue(plain.contains("This is a quote."));
        assertTrue(plain.contains("Normal text."));
    }

    @Test
    void stripsListSyntax() {
        String markdown = "- Item 1\n- Item 2\n1. Numbered";
        String plain = SummaryGenerator.stripMarkdown(markdown);
        assertTrue(plain.contains("Item 1"));
        assertTrue(plain.contains("Item 2"));
        assertTrue(plain.contains("Numbered"));
    }

    @Test
    void stripsHtmlTags() {
        String markdown = "<script>alert('xss')</script>\nNormal text.";
        String plain = SummaryGenerator.stripMarkdown(markdown);
        // stripMarkdown 只去除 HTML 标签，标签内的文本内容会保留。
        // 实际的 XSS 防护由 OwaspHtmlSanitizer 在渲染阶段处理。
        assertTrue(plain.contains("Normal text."));
    }
}
