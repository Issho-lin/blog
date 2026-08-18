package com.linqibin.blog.post.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.linqibin.blog.media.application.MediaService;
import com.linqibin.blog.media.exception.InvalidFileException;
import com.linqibin.blog.media.infrastructure.FileStorageService;
import com.linqibin.blog.markdown.exporter.FrontMatterExporter;
import com.linqibin.blog.markdown.parser.FrontMatterParser;
import com.linqibin.blog.post.domain.SlugGenerator;
import com.linqibin.blog.post.infrastructure.InMemoryPostRepository;
import com.linqibin.blog.taxonomy.application.CategoryService;
import com.linqibin.blog.taxonomy.application.TagService;
import com.linqibin.blog.taxonomy.infrastructure.InMemoryCategoryRepository;
import com.linqibin.blog.taxonomy.infrastructure.InMemoryTagRepository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostImportExportEdgeCaseTest {

    private PostImportExportService importExportService;

    @BeforeEach
    void setUp() {
        InMemoryPostRepository postRepository = new InMemoryPostRepository();
        SlugGenerator slugGenerator = new SlugGenerator(() -> "fallback-1");
        Clock clock = Clock.fixed(Instant.parse("2026-08-11T10:00:00Z"), ZoneOffset.UTC);
        AtomicInteger idCounter = new AtomicInteger(1);
        Supplier<UUID> idSupplier = () -> UUID.nameUUIDFromBytes(
                ("edge-" + idCounter.getAndIncrement()).getBytes(StandardCharsets.UTF_8)
        );

        PostService postService = new PostService(postRepository, slugGenerator, clock, idSupplier);
        CategoryService categoryService = new CategoryService(
                new InMemoryCategoryRepository(), slugGenerator, clock, idSupplier
        );
        TagService tagService = new TagService(
                new InMemoryTagRepository(), slugGenerator, clock, idSupplier
        );

        importExportService = new PostImportExportService(
                postService,
                categoryService,
                tagService,
                new FrontMatterParser(),
                new FrontMatterExporter(),
                slugGenerator,
                memoryMediaService()
        );
    }

    @Test
    void importEmptyFileThrowsInvalidFileException() {
        assertThrows(
                InvalidFileException.class,
                () -> importExportService.importMarkdown("empty.md", new byte[0])
        );
    }

    @Test
    void importNullFilenameThrowsInvalidFileException() {
        assertThrows(
                InvalidFileException.class,
                () -> importExportService.importMarkdown(null, "# body".getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void importChineseContentSucceedsWithoutException() {
        byte[] content = """
                ---
                title: 中文标题
                ---
                # 你好，世界
                """.getBytes(StandardCharsets.UTF_8);

        var post = importExportService.importMarkdown("chinese.md", content);

        assertEquals("中文标题", post.title());
        assertTrue(post.markdownContent().contains("你好，世界"));
    }

    private static MediaService memoryMediaService() {
        FileStorageService storage = new FileStorageService() {
            @Override
            public String store(String storedFilename, InputStream content) {
                return "/uploads/" + storedFilename;
            }

            @Override
            public void delete(String storedFilename) {
            }
        };
        return new MediaService(storage, Clock.systemUTC());
    }
}
