package com.linqibin.blog.post.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlugGeneratorTest {

    private SlugGenerator slugGenerator;

    @BeforeEach
    void setUp() {
        AtomicInteger fallbackCounter = new AtomicInteger(1);
        slugGenerator = new SlugGenerator(() -> "fb" + fallbackCounter.getAndIncrement());
    }

    @Test
    void generateFromTitleNormalizesEnglishTitle() {
        assertEquals("hello-spring-boot", slugGenerator.generateFromTitle(" Hello, Spring Boot! "));
    }

    @Test
    void generateFromTitleFallsBackForChineseTitle() {
        String slug = slugGenerator.generateFromTitle("你好，个人博客");

        assertTrue(slug.startsWith("post-fb"));
    }

    @Test
    void generateFromTitleFallsBackForBlankTitle() {
        String slug = slugGenerator.generateFromTitle("   ");

        assertTrue(slug.startsWith("post-fb"));
    }

    @Test
    void normalizeRequestedSlugTrimsAndLowercases() {
        assertEquals("my-custom-slug", slugGenerator.normalizeRequestedSlug("  My-Custom-Slug  "));
    }

    @Test
    void normalizeRequestedSlugRejectsBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> slugGenerator.normalizeRequestedSlug("   "));
    }

    @Test
    void ensureUniqueReturnsBaseSlugWhenAvailable() {
        assertEquals("hello-world", slugGenerator.ensureUnique("hello-world", slug -> false));
    }

    @Test
    void ensureUniqueAppendsNumericSuffixWhenTaken() {
        Set<String> taken = new HashSet<>();
        taken.add("hello-world");

        assertEquals("hello-world-2", slugGenerator.ensureUnique("hello-world", taken::contains));
    }

    @Test
    void ensureUniqueSkipsMultipleTakenSuffixes() {
        Set<String> taken = new HashSet<>();
        taken.add("hello-world");
        taken.add("hello-world-2");
        taken.add("hello-world-3");

        assertEquals("hello-world-4", slugGenerator.ensureUnique("hello-world", taken::contains));
    }

    @Test
    void generateFromTitleTrimsToMaxLength() {
        String longTitle = "a".repeat(200);

        String slug = slugGenerator.generateFromTitle(longTitle);

        assertEquals(120, slug.length());
    }
}
