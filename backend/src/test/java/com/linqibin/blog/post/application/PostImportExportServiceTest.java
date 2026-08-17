package com.linqibin.blog.post.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.linqibin.blog.markdown.exporter.FrontMatterExporter;
import com.linqibin.blog.markdown.parser.FrontMatterParser;
import com.linqibin.blog.media.exception.InvalidFileException;
import com.linqibin.blog.post.domain.Post;
import com.linqibin.blog.post.domain.PostStatus;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;
import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.taxonomy.domain.Category;
import com.linqibin.blog.taxonomy.domain.Tag;
import com.linqibin.blog.taxonomy.infrastructure.InMemoryCategoryRepository;
import com.linqibin.blog.taxonomy.infrastructure.InMemoryTagRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostImportExportServiceTest {

    private PostImportExportService importExportService;
    private PostService postService;
    private CategoryService categoryService;
    private TagService tagService;
    private InMemoryPostRepository postRepository;

    @BeforeEach
    void setUp() {
        this.postRepository = new InMemoryPostRepository();
        SlugGenerator slugGenerator = new SlugGenerator(() -> "fallback-1");
        Clock clock = Clock.fixed(Instant.parse("2026-08-11T10:00:00Z"), ZoneOffset.UTC);
        AtomicInteger idCounter = new AtomicInteger(1);
        Supplier<UUID> idSupplier = () -> UUID.nameUUIDFromBytes(
                ("id-" + idCounter.getAndIncrement()).getBytes(StandardCharsets.UTF_8)
        );

        this.postService = new PostService(postRepository, slugGenerator, clock, idSupplier);

        InMemoryCategoryRepository categoryRepo = new InMemoryCategoryRepository();
        InMemoryTagRepository tagRepo = new InMemoryTagRepository();
        this.categoryService = new CategoryService(categoryRepo, slugGenerator, clock, idSupplier);
        this.tagService = new TagService(tagRepo, slugGenerator, clock, idSupplier);

        // 预置一个分类和标签供导入回填。
        categoryService.create("Spring Boot", "spring-boot", null);
        tagService.create("Java", "java");

        this.importExportService = new PostImportExportService(
                postService, categoryService, tagService,
                new FrontMatterParser(), new FrontMatterExporter(), slugGenerator
        );
    }

    @Test
    void importWithFrontMatterCreatesDraftWithMetadata() {
        byte[] content = """
                ---
                title: Imported Post
                slug: imported-post
                description: A description
                category: spring-boot
                tags:
                  - java
                status: published
                ---
                # Hello World

                Content here.
                """.getBytes(StandardCharsets.UTF_8);

        Post post = importExportService.importMarkdown("imported-post.md", content);

        assertEquals("Imported Post", post.title());
        assertEquals("imported-post", post.slug());
        assertEquals("# Hello World\n\nContent here.", post.markdownContent().trim());
        assertEquals(PostStatus.DRAFT, post.status()); // 导入永远创建草稿
        assertNotNull(post.categoryId());
        assertEquals(1, post.tagIds().size());
        assertEquals("A description", post.excerpt());
    }

    @Test
    void importIgnoresStatusPublished() {
        byte[] content = """
                ---
                title: Status Test
                status: published
                ---
                Body
                """.getBytes(StandardCharsets.UTF_8);

        Post post = importExportService.importMarkdown("test.md", content);

        assertEquals(PostStatus.DRAFT, post.status());
    }

    @Test
    void importWithoutFrontMatterUsesFilenameAsTitle() {
        byte[] content = "# Just Content\n\nNo front matter.".getBytes(StandardCharsets.UTF_8);

        Post post = importExportService.importMarkdown("my-article.md", content);

        assertEquals("my-article", post.title());
        assertEquals(PostStatus.DRAFT, post.status());
    }

    @Test
    void importWithoutSlugAutoGeneratesSlug() {
        byte[] content = """
                ---
                title: Auto Slug
                ---
                Body
                """.getBytes(StandardCharsets.UTF_8);

        Post post = importExportService.importMarkdown("test.md", content);

        assertEquals("auto-slug", post.slug());
    }

    @Test
    void importWithDuplicateSlugAppendsSuffix() {
        byte[] first = """
                ---
                title: Duplicate
                slug: same-slug
                ---
                First
                """.getBytes(StandardCharsets.UTF_8);
        importExportService.importMarkdown("first.md", first);

        byte[] second = """
                ---
                title: Duplicate
                slug: same-slug
                ---
                Second
                """.getBytes(StandardCharsets.UTF_8);
        Post post = importExportService.importMarkdown("second.md", second);

        assertTrue(post.slug().startsWith("same-slug"));
        assertTrue(post.slug().length() > "same-slug".length());
    }

    @Test
    void importWithUnknownCategoryLeavesNull() {
        byte[] content = """
                ---
                title: Unknown Category
                category: non-existent
                ---
                Body
                """.getBytes(StandardCharsets.UTF_8);

        Post post = importExportService.importMarkdown("test.md", content);

        assertNotNull(post);
        assertTrue(post.categoryId() == null);
    }

    @Test
    void importWithInvalidExtensionThrows() {
        byte[] content = "content".getBytes(StandardCharsets.UTF_8);

        assertThrows(
                InvalidFileException.class,
                () -> importExportService.importMarkdown("file.txt", content)
        );
    }

    @Test
    void exportPostGeneratesMarkdownWithFrontMatter() {
        // 先创建一篇带分类和标签的文章。
        UUID categoryId = categoryService.findAll().get(0).id();
        UUID tagId = tagService.findAll().get(0).id();
        Post post = postService.createDraft("Export Me", "# content", "export-me", categoryId, java.util.List.of(tagId));

        String exported = importExportService.exportPost(post.id());

        assertTrue(exported.startsWith("---\n"));
        assertTrue(exported.contains("title: Export Me"));
        assertTrue(exported.contains("slug: export-me"));
        assertTrue(exported.contains("category: Spring Boot"));
        assertTrue(exported.contains("- Java"));
        assertTrue(exported.contains("status: DRAFT"));
        assertTrue(exported.contains("# content"));
    }

    @Test
    void importAndExportPreserveExcerptAndCover() {
        byte[] content = """
                ---
                title: Cover Import
                slug: cover-import
                excerpt: Front matter excerpt
                cover: /uploads/from-import.jpg
                ---
                Body
                """.getBytes(StandardCharsets.UTF_8);

        Post imported = importExportService.importMarkdown("cover-import.md", content);

        assertEquals("Front matter excerpt", imported.excerpt());
        assertEquals("/uploads/from-import.jpg", imported.coverUrl());

        String exported = importExportService.exportPost(imported.id());
        assertTrue(exported.contains("excerpt: Front matter excerpt"));
        assertTrue(exported.contains("cover: /uploads/from-import.jpg"));
    }

    @Test
    void exportPostWithFilenameReturnsCorrectFilename() {
        Post post = postService.createDraft("Filename Test", "# content", "filename-test", null, null);

        var result = importExportService.exportPostWithFilename(post.id());

        assertEquals("filename-test.md", result.filename());
        assertTrue(result.markdown().contains("title: Filename Test"));
    }

    @Test
    void roundTripExportThenImportPreservesTitleAndBody() {
        // 创建原始文章。
        Post original = postService.createDraft("Round Trip", "# Original Content", "round-trip", null, null);

        // 导出。
        String exported = importExportService.exportPost(original.id());

        // 重新导入。
        Post reimported = importExportService.importMarkdown(
                "round-trip.md", exported.getBytes(StandardCharsets.UTF_8)
        );

        // 原始文章仍存在于仓库中，导入时自动追加数字后缀保证唯一。
        assertEquals("Round Trip", reimported.title());
        assertTrue(reimported.slug().startsWith("round-trip"));
        assertTrue(reimported.markdownContent().contains("# Original Content"));
        assertEquals(PostStatus.DRAFT, reimported.status());
    }
}
