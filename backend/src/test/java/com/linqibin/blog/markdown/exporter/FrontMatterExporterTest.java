package com.linqibin.blog.markdown.exporter;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.linqibin.blog.markdown.parser.FrontMatterParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontMatterExporterTest {

    private final FrontMatterExporter exporter = new FrontMatterExporter();
    private final FrontMatterParser parser = new FrontMatterParser();

    @Test
    void exportGeneratesFrontMatterAndBody() {
        String result = exporter.export(
                "Hello World",
                "hello-world",
                "A test post",
                "/images/cover.jpg",
                "Spring Boot",
                List.of("java", "spring"),
                "PUBLISHED",
                Instant.parse("2026-08-11T10:00:00Z"),
                Instant.parse("2026-08-11T11:00:00Z"),
                "# Hello\n\nContent"
        );

        assertTrue(result.startsWith("---\n"));
        assertTrue(result.contains("title: Hello World"));
        assertTrue(result.contains("slug: hello-world"));
        assertTrue(result.contains("excerpt: A test post"));
        assertTrue(result.contains("cover: /images/cover.jpg"));
        assertTrue(result.contains("category: Spring Boot"));
        assertTrue(result.contains("tags:"));
        assertTrue(result.contains("- java"));
        assertTrue(result.contains("- spring"));
        assertTrue(result.contains("published_at: 2026-08-11T10:00:00Z"));
        assertTrue(result.contains("status: PUBLISHED"));
        assertTrue(result.contains("updated_at: 2026-08-11T11:00:00Z"));
        assertTrue(result.endsWith("# Hello\n\nContent"));
    }

    @Test
    void exportOmitsNullOptionalFields() {
        String result = exporter.export(
                "Test",
                "test",
                null,
                null,
                null,
                null,
                "DRAFT",
                null,
                Instant.parse("2026-08-11T10:00:00Z"),
                "Body"
        );

        assertTrue(result.contains("title: Test"));
        assertTrue(result.contains("slug: test"));
        assertFalseContains(result, "excerpt:");
        assertFalseContains(result, "cover:");
        assertFalseContains(result, "category:");
        assertFalseContains(result, "tags:");
        assertFalseContains(result, "published_at:");
        assertTrue(result.contains("status: DRAFT"));
    }

    @Test
    void roundTripPreservesTitleSlugAndBody() {
        String exported = exporter.export(
                "Round Trip",
                "round-trip",
                "Test description",
                null,
                null,
                List.of("tag1", "tag2"),
                "DRAFT",
                null,
                Instant.parse("2026-08-11T10:00:00Z"),
                "# Content\n\nParagraph."
        );

        var parsed = parser.parse(exported);

        assertEquals("Round Trip", parsed.frontMatter().title());
        assertEquals("round-trip", parsed.frontMatter().slug());
        assertEquals("Test description", parsed.frontMatter().description());
        assertEquals(List.of("tag1", "tag2"), parsed.frontMatter().tags());
        assertTrue(parsed.body().startsWith("# Content"));
    }

    private void assertFalseContains(String haystack, String needle) {
        assertTrue(!haystack.contains(needle), "Should not contain: " + needle);
    }
}
