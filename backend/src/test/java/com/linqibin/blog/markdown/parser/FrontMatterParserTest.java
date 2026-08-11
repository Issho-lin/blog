package com.linqibin.blog.markdown.parser;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontMatterParserTest {

    private final FrontMatterParser parser = new FrontMatterParser();

    @Test
    void parseWithFrontMatterExtractsAllFields() {
        String markdown = """
                ---
                title: Hello World
                slug: hello-world
                description: A test post
                cover: /images/cover.jpg
                date: 2026-08-11T10:00:00Z
                category: spring-boot
                tags:
                  - java
                  - spring
                status: published
                ---
                # Hello World

                This is the content.
                """;

        var result = parser.parse(markdown);

        assertTrue(result.hasFrontMatter());
        assertEquals("Hello World", result.frontMatter().title());
        assertEquals("hello-world", result.frontMatter().slug());
        assertEquals("A test post", result.frontMatter().description());
        assertEquals("/images/cover.jpg", result.frontMatter().cover());
        assertEquals("2026-08-11T10:00:00Z", result.frontMatter().date());
        assertEquals("spring-boot", result.frontMatter().category());
        assertEquals(List.of("java", "spring"), result.frontMatter().tags());
        assertEquals("published", result.frontMatter().status());
        assertTrue(result.body().startsWith("# Hello World"));
    }

    @Test
    void parseWithoutFrontMatterReturnsFullContentAsBody() {
        String markdown = "# Just a title\n\nSome content.";

        var result = parser.parse(markdown);

        assertFalse(result.hasFrontMatter());
        assertNull(result.frontMatter().title());
        assertEquals(markdown, result.body());
    }

    @Test
    void parseSupportsExcerptAsAliasForDescription() {
        String markdown = """
                ---
                title: Test
                excerpt: From excerpt field
                ---
                Body
                """;

        var result = parser.parse(markdown);

        assertEquals("From excerpt field", result.frontMatter().description());
    }

    @Test
    void parseSupportsPublishedAtAsAliasForDate() {
        String markdown = """
                ---
                title: Test
                published_at: 2026-01-01
                ---
                Body
                """;

        var result = parser.parse(markdown);

        assertEquals("2026-01-01", result.frontMatter().date());
    }

    @Test
    void parseSupportsTagsAsCommaSeparatedString() {
        String markdown = """
                ---
                title: Test
                tags: java, spring, docker
                ---
                Body
                """;

        var result = parser.parse(markdown);

        assertEquals(List.of("java", "spring", "docker"), result.frontMatter().tags());
    }

    @Test
    void parseIgnoresUnknownFields() {
        String markdown = """
                ---
                title: Test
                unknown_field: some value
                another_unknown: 123
                ---
                Body
                """;

        var result = parser.parse(markdown);

        assertEquals("Test", result.frontMatter().title());
        assertTrue(result.hasFrontMatter());
    }

    @Test
    void parseHandlesEmptyFrontMatter() {
        String markdown = """
                ---
                ---
                Body content here.
                """;

        var result = parser.parse(markdown);

        assertTrue(result.hasFrontMatter());
        assertNull(result.frontMatter().title());
        assertEquals("Body content here.", result.body().trim());
    }

    @Test
    void parseHandlesBom() {
        String markdown = "\uFEFF---\ntitle: BOM Test\n---\nBody";

        var result = parser.parse(markdown);

        assertTrue(result.hasFrontMatter());
        assertEquals("BOM Test", result.frontMatter().title());
        assertEquals("Body", result.body().trim());
    }

    @Test
    void parseHandlesMissingEndDelimiter() {
        String markdown = "---\ntitle: No End\nThis is not front matter.";

        var result = parser.parse(markdown);

        assertFalse(result.hasFrontMatter());
    }

    @Test
    void parseHandlesFrontMatterWithNoBody() {
        String markdown = "---\ntitle: Just Front Matter\n---";

        var result = parser.parse(markdown);

        assertTrue(result.hasFrontMatter());
        assertEquals("Just Front Matter", result.frontMatter().title());
        assertTrue(result.body().isEmpty());
    }
}
