package com.linqibin.blog.markdown.sanitizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwaspHtmlSanitizerTest {

    private final OwaspHtmlSanitizer sanitizer = new OwaspHtmlSanitizer();

    @Test
    void keepsSafeParagraphAndHeading() {
        String html = "<h1>Title</h1><p>Paragraph</p>";
        String result = sanitizer.sanitize(html);

        assertTrue(result.contains("<h1>Title</h1>"));
        assertTrue(result.contains("<p>Paragraph</p>"));
    }

    @Test
    void removesScriptTag() {
        String html = "<p>safe</p><script>alert('xss')</script>";
        String result = sanitizer.sanitize(html);

        assertTrue(result.contains("safe"));
        assertFalse(result.contains("<script"));
        assertFalse(result.contains("alert"));
    }

    @Test
    void removesOnEventAttributes() {
        String html = "<p onclick=\"steal()\">text</p>";
        String result = sanitizer.sanitize(html);

        assertTrue(result.contains("text"));
        assertFalse(result.contains("onclick"));
        assertFalse(result.contains("steal"));
    }

    @Test
    void removesJavascriptUrlProtocol() {
        String html = "<a href=\"javascript:steal()\">click</a>";
        String result = sanitizer.sanitize(html);

        assertFalse(result.contains("javascript:"));
        assertFalse(result.contains("steal"));
    }

    @Test
    void keepsSafeHttpLink() {
        String html = "<a href=\"https://example.com\">link</a>";
        String result = sanitizer.sanitize(html);

        assertTrue(result.contains("https://example.com"));
        assertTrue(result.contains("link"));
    }

    @Test
    void removesIframeTag() {
        String html = "<iframe src=\"https://evil.com\"></iframe><p>safe</p>";
        String result = sanitizer.sanitize(html);

        assertFalse(result.contains("<iframe"));
        assertFalse(result.contains("evil.com"));
        assertTrue(result.contains("safe"));
    }

    @Test
    void removesStyleAttribute() {
        String html = "<p style=\"color:red\">styled text</p>";
        String result = sanitizer.sanitize(html);

        assertTrue(result.contains("styled text"));
        assertFalse(result.contains("style="));
        assertFalse(result.contains("color:red"));
    }

    @Test
    void removesObjectAndEmbedTags() {
        String html = "<object data=\"evil.swf\"></object><embed src=\"evil.swf\"><p>safe</p>";
        String result = sanitizer.sanitize(html);

        assertFalse(result.contains("<object"));
        assertFalse(result.contains("<embed"));
        assertTrue(result.contains("safe"));
    }

    @Test
    void removesMetaRefreshTag() {
        String html = "<meta http-equiv=\"refresh\" content=\"0;url=evil.com\"><p>safe</p>";
        String result = sanitizer.sanitize(html);

        assertFalse(result.contains("<meta"));
        assertFalse(result.contains("refresh"));
        assertTrue(result.contains("safe"));
    }

    @Test
    void addsRelNofollowToLinks() {
        String html = "<a href=\"https://example.com\">link</a>";
        String result = sanitizer.sanitize(html);

        assertTrue(result.contains("rel=\"nofollow\""));
    }

    @Test
    void returnsEmptyForNullInput() {
        assertEquals("", sanitizer.sanitize(null));
    }

    @Test
    void returnsEmptyForBlankInput() {
        assertEquals("", sanitizer.sanitize("   "));
    }
}
