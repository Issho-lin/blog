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
import com.linqibin.blog.taxonomy.domain.Category;
import com.linqibin.blog.taxonomy.exception.CategoryNotFoundException;
import com.linqibin.blog.taxonomy.exception.DuplicateTaxonomySlugException;
import com.linqibin.blog.taxonomy.infrastructure.InMemoryCategoryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryServiceTest {

    private InMemoryCategoryRepository categoryRepository;
    private SlugGenerator slugGenerator;
    private Supplier<UUID> idSupplier;
    private AtomicInteger categoryIdCounter;
    private AtomicInteger fallbackSlugCounter;

    @BeforeEach
    void setUp() {
        this.categoryRepository = new InMemoryCategoryRepository();
        this.categoryIdCounter = new AtomicInteger(1);
        this.fallbackSlugCounter = new AtomicInteger(1);
        this.idSupplier = () -> UUID.nameUUIDFromBytes(
                ("category-" + categoryIdCounter.getAndIncrement()).getBytes(StandardCharsets.UTF_8)
        );
        this.slugGenerator = new SlugGenerator(() -> "fallback-" + fallbackSlugCounter.getAndIncrement());
    }

    @Test
    void createGeneratesSlugFromEnglishName() {
        CategoryService service = createServiceAt("2026-08-11T10:00:00Z");

        Category category = service.create(" Hello, Spring Boot! ", null, "A framework");

        assertEquals("Hello, Spring Boot!", category.name());
        assertEquals("hello-spring-boot", category.slug());
        assertEquals("A framework", category.description());
        assertEquals(Instant.parse("2026-08-11T10:00:00Z"), category.createdAt());
        assertEquals(Instant.parse("2026-08-11T10:00:00Z"), category.updatedAt());
    }

    @Test
    void createWithChineseNameFallsBackToShortIdSlug() {
        CategoryService service = createServiceAt("2026-08-11T10:00:00Z");

        Category category = service.create("你好，博客", null, null);

        assertEquals("post-fallback-1", category.slug());
    }

    @Test
    void createWithDuplicateGeneratedSlugAppendsNumericSuffix() {
        CategoryService service = createServiceAt("2026-08-11T10:00:00Z");

        Category first = service.create("Hello World", null, null);
        Category second = service.create("Hello World", null, null);

        assertEquals("hello-world", first.slug());
        assertEquals("hello-world-2", second.slug());
    }

    @Test
    void createWithDuplicateRequestedSlugThrowsException() {
        CategoryService service = createServiceAt("2026-08-11T10:00:00Z");

        service.create("First", "custom-slug", null);

        assertThrows(
                DuplicateTaxonomySlugException.class,
                () -> service.create("Second", "custom-slug", null)
        );
    }

    @Test
    void createWithRequestedSlugUsesNormalizedSlug() {
        CategoryService service = createServiceAt("2026-08-11T10:00:00Z");

        Category category = service.create("My Category", "  Custom Slug Here  ", null);

        assertEquals("custom-slug-here", category.slug());
    }

    @Test
    void updateChangesNameAndDescriptionButKeepsSlug() {
        CategoryService createService = createServiceAt("2026-08-11T10:00:00Z");
        Category created = createService.create("Original", null, "old description");
        CategoryService updateService = createServiceAt("2026-08-11T11:00:00Z");

        Category updated = updateService.update(created.id(), "Updated Name", "new description");

        assertEquals("Updated Name", updated.name());
        assertEquals("new description", updated.description());
        assertEquals("original", updated.slug());
        assertEquals(Instant.parse("2026-08-11T10:00:00Z"), updated.createdAt());
        assertEquals(Instant.parse("2026-08-11T11:00:00Z"), updated.updatedAt());
    }

    @Test
    void updateSlugChangesSlugWhenNewSlugIsUnique() {
        CategoryService createService = createServiceAt("2026-08-11T10:00:00Z");
        Category created = createService.create("My Category", null, null);
        CategoryService updateService = createServiceAt("2026-08-11T11:00:00Z");

        Category updated = updateService.updateSlug(created.id(), "new-category-slug");

        assertEquals("new-category-slug", updated.slug());
        assertEquals(Instant.parse("2026-08-11T11:00:00Z"), updated.updatedAt());
    }

    @Test
    void updateSlugWithSameSlugDoesNotThrow() {
        CategoryService createService = createServiceAt("2026-08-11T10:00:00Z");
        Category created = createService.create("My Category", "fixed-slug", null);
        CategoryService updateService = createServiceAt("2026-08-11T11:00:00Z");

        Category updated = updateService.updateSlug(created.id(), "fixed-slug");

        assertEquals("fixed-slug", updated.slug());
    }

    @Test
    void updateSlugWithDuplicateSlugThrowsException() {
        CategoryService createService = createServiceAt("2026-08-11T10:00:00Z");
        createService.create("First", "taken-slug", null);
        Category second = createService.create("Second", "another-slug", null);
        CategoryService updateService = createServiceAt("2026-08-11T11:00:00Z");

        assertThrows(
                DuplicateTaxonomySlugException.class,
                () -> updateService.updateSlug(second.id(), "taken-slug")
        );
    }

    @Test
    void getCategoryByIdThrowsWhenNotFound() {
        CategoryService service = createServiceAt("2026-08-11T10:00:00Z");

        assertThrows(
                CategoryNotFoundException.class,
                () -> service.getCategory(UUID.randomUUID())
        );
    }

    @Test
    void getCategoryBySlugThrowsWhenNotFound() {
        CategoryService service = createServiceAt("2026-08-11T10:00:00Z");

        assertThrows(
                CategoryNotFoundException.class,
                () -> service.getCategoryBySlug("non-existent")
        );
    }

    @Test
    void findAllReturnsAllCategories() {
        CategoryService service = createServiceAt("2026-08-11T10:00:00Z");
        service.create("First", null, null);
        service.create("Second", null, null);

        var categories = service.findAll();

        assertEquals(2, categories.size());
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("First")));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("Second")));
    }

    @Test
    void deleteRemovesCategory() {
        CategoryService service = createServiceAt("2026-08-11T10:00:00Z");
        Category created = service.create("Delete Me", null, null);

        service.delete(created.id());

        assertTrue(categoryRepository.findById(created.id()).isEmpty());
    }

    private CategoryService createServiceAt(String instantValue) {
        return new CategoryService(
                categoryRepository,
                slugGenerator,
                Clock.fixed(Instant.parse(instantValue), ZoneOffset.UTC),
                idSupplier
        );
    }
}
