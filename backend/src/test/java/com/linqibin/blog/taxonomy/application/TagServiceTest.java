package com.linqibin.blog.taxonomy.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.taxonomy.domain.Tag;
import com.linqibin.blog.taxonomy.exception.DuplicateTaxonomySlugException;
import com.linqibin.blog.taxonomy.exception.TagNotFoundException;
import com.linqibin.blog.taxonomy.infrastructure.InMemoryTagRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagServiceTest {

    private InMemoryTagRepository tagRepository;
    private SlugGenerator slugGenerator;
    private Supplier<UUID> idSupplier;
    private AtomicInteger tagIdCounter;
    private AtomicInteger fallbackSlugCounter;

    @BeforeEach
    void setUp() {
        this.tagRepository = new InMemoryTagRepository();
        this.tagIdCounter = new AtomicInteger(1);
        this.fallbackSlugCounter = new AtomicInteger(1);
        this.idSupplier = () -> UUID.nameUUIDFromBytes(
                ("tag-" + tagIdCounter.getAndIncrement()).getBytes(StandardCharsets.UTF_8)
        );
        this.slugGenerator = new SlugGenerator(() -> "fallback-" + fallbackSlugCounter.getAndIncrement());
    }

    @Test
    void createGeneratesSlugFromEnglishName() {
        TagService service = createServiceAt("2026-08-11T10:00:00Z");

        Tag tag = service.create(" Hello, Spring Boot! ", null);

        assertEquals("Hello, Spring Boot!", tag.name());
        assertEquals("hello-spring-boot", tag.slug());
        assertEquals(Instant.parse("2026-08-11T10:00:00Z"), tag.createdAt());
        assertEquals(Instant.parse("2026-08-11T10:00:00Z"), tag.updatedAt());
    }

    @Test
    void createWithChineseNameFallsBackToShortIdSlug() {
        TagService service = createServiceAt("2026-08-11T10:00:00Z");

        Tag tag = service.create("你好，博客", null);

        assertEquals("post-fallback-1", tag.slug());
    }

    @Test
    void createWithDuplicateGeneratedSlugAppendsNumericSuffix() {
        TagService service = createServiceAt("2026-08-11T10:00:00Z");

        Tag first = service.create("Hello World", null);
        Tag second = service.create("Hello World", null);

        assertEquals("hello-world", first.slug());
        assertEquals("hello-world-2", second.slug());
    }

    @Test
    void createWithDuplicateRequestedSlugThrowsException() {
        TagService service = createServiceAt("2026-08-11T10:00:00Z");

        service.create("First", "custom-slug");

        assertThrows(
                DuplicateTaxonomySlugException.class,
                () -> service.create("Second", "custom-slug")
        );
    }

    @Test
    void createWithRequestedSlugUsesNormalizedSlug() {
        TagService service = createServiceAt("2026-08-11T10:00:00Z");

        Tag tag = service.create("My Tag", "  Custom Slug Here  ");

        assertEquals("custom-slug-here", tag.slug());
    }

    @Test
    void updateChangesNameButKeepsSlug() {
        TagService createService = createServiceAt("2026-08-11T10:00:00Z");
        Tag created = createService.create("Original", null);
        TagService updateService = createServiceAt("2026-08-11T11:00:00Z");

        Tag updated = updateService.update(created.id(), "Updated Name");

        assertEquals("Updated Name", updated.name());
        assertEquals("original", updated.slug());
        assertEquals(Instant.parse("2026-08-11T10:00:00Z"), updated.createdAt());
        assertEquals(Instant.parse("2026-08-11T11:00:00Z"), updated.updatedAt());
    }

    @Test
    void updateSlugChangesSlugWhenNewSlugIsUnique() {
        TagService createService = createServiceAt("2026-08-11T10:00:00Z");
        Tag created = createService.create("My Tag", null);
        TagService updateService = createServiceAt("2026-08-11T11:00:00Z");

        Tag updated = updateService.updateSlug(created.id(), "new-tag-slug");

        assertEquals("new-tag-slug", updated.slug());
        assertEquals(Instant.parse("2026-08-11T11:00:00Z"), updated.updatedAt());
    }

    @Test
    void updateSlugWithSameSlugDoesNotThrow() {
        TagService createService = createServiceAt("2026-08-11T10:00:00Z");
        Tag created = createService.create("My Tag", "fixed-slug");
        TagService updateService = createServiceAt("2026-08-11T11:00:00Z");

        Tag updated = updateService.updateSlug(created.id(), "fixed-slug");

        assertEquals("fixed-slug", updated.slug());
    }

    @Test
    void updateSlugWithDuplicateSlugThrowsException() {
        TagService createService = createServiceAt("2026-08-11T10:00:00Z");
        createService.create("First", "taken-slug");
        Tag second = createService.create("Second", "another-slug");
        TagService updateService = createServiceAt("2026-08-11T11:00:00Z");

        assertThrows(
                DuplicateTaxonomySlugException.class,
                () -> updateService.updateSlug(second.id(), "taken-slug")
        );
    }

    @Test
    void getTagByIdThrowsWhenNotFound() {
        TagService service = createServiceAt("2026-08-11T10:00:00Z");

        assertThrows(
                TagNotFoundException.class,
                () -> service.getTag(UUID.randomUUID())
        );
    }

    @Test
    void getTagBySlugThrowsWhenNotFound() {
        TagService service = createServiceAt("2026-08-11T10:00:00Z");

        assertThrows(
                TagNotFoundException.class,
                () -> service.getTagBySlug("non-existent")
        );
    }

    @Test
    void findAllReturnsAllTags() {
        TagService service = createServiceAt("2026-08-11T10:00:00Z");
        service.create("First", null);
        service.create("Second", null);

        var tags = service.findAll();

        assertEquals(2, tags.size());
        assertTrue(tags.stream().anyMatch(t -> t.name().equals("First")));
        assertTrue(tags.stream().anyMatch(t -> t.name().equals("Second")));
    }

    @Test
    void deleteRemovesTag() {
        TagService service = createServiceAt("2026-08-11T10:00:00Z");
        Tag created = service.create("Delete Me", null);

        service.delete(created.id());

        assertTrue(tagRepository.findById(created.id()).isEmpty());
    }

    private TagService createServiceAt(String instantValue) {
        return new TagService(
                tagRepository,
                slugGenerator,
                Clock.fixed(Instant.parse(instantValue), ZoneOffset.UTC),
                idSupplier
        );
    }
}
