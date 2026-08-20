package com.linqibin.blog.ai.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import com.linqibin.blog.ai.web.AiTaxonomyResponse;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.taxonomy.infrastructure.InMemoryCategoryRepository;
import com.linqibin.blog.taxonomy.infrastructure.InMemoryTagRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiTaxonomyServiceTest {

    private CategoryService categoryService;
    private TagService tagService;
    private AiTaxonomyService taxonomyService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC);
        AtomicInteger ids = new AtomicInteger(1);
        SlugGenerator slugGenerator = new SlugGenerator(() -> "fallback-" + ids.get());
        categoryService = new CategoryService(
                new InMemoryCategoryRepository(),
                slugGenerator,
                clock,
                () -> UUID.nameUUIDFromBytes(("c-" + ids.getAndIncrement()).getBytes(StandardCharsets.UTF_8))
        );
        tagService = new TagService(
                new InMemoryTagRepository(),
                new SlugGenerator(() -> "tag-fallback-" + ids.get()),
                clock,
                () -> UUID.nameUUIDFromBytes(("t-" + ids.getAndIncrement()).getBytes(StandardCharsets.UTF_8))
        );
        taxonomyService = new AiTaxonomyService(
                null,
                categoryService,
                tagService,
                JsonMapper.builder().build()
        );
    }

    @Test
    void reusesExistingNamesAndCreatesMissingOnes() {
        categoryService.create("工程实践", null, "");
        tagService.create("Java", null);

        AiTaxonomyResponse result = taxonomyService.apply(
                new AiTaxonomyParser.Suggestion("工程实践", List.of("Java", "缓存"))
        );

        assertFalse(result.categoryCreated());
        assertEquals("工程实践", result.categoryName());
        assertEquals(2, result.tags().size());
        assertFalse(result.tags().get(0).created());
        assertTrue(result.tags().get(1).created());
        assertEquals("缓存", result.tags().get(1).name());
        assertEquals(2, tagService.findAll().size());
        assertEquals(1, categoryService.findAll().size());
    }
}
