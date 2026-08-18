package com.linqibin.blog.media.application;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.linqibin.blog.media.exception.InvalidFileException;
import com.linqibin.blog.media.infrastructure.FileStorageService;
import com.linqibin.blog.post.domain.Post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaServiceTest {

    private FileStorageService fileStorageService;
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        this.fileStorageService = new InMemoryFileStorageService();
        this.mediaService = new MediaService(
                fileStorageService,
                Clock.fixed(Instant.parse("2026-08-11T10:00:00Z"), ZoneOffset.UTC),
                1024 // 1KB max for testing
        );
    }

    @Test
    void uploadValidImageReturnsUrl() {
        byte[] content = new byte[100];
        var result = mediaService.uploadImage("photo.png", "image/png", 100, new ByteArrayInputStream(content));

        assertNotNull(result.url());
        assertTrue(result.url().startsWith("/uploads/"));
        assertEquals("photo.png", result.originalFilename());
        assertEquals("image/png", result.contentType());
        assertEquals(100, result.size());
    }

    @Test
    void uploadJpegImageSucceeds() {
        byte[] content = new byte[100];
        var result = mediaService.uploadImage("photo.jpg", "image/jpeg", 100, new ByteArrayInputStream(content));

        assertNotNull(result.url());
        assertTrue(result.url().endsWith(".jpg"));
    }

    @Test
    void uploadWebpImageSucceeds() {
        byte[] content = new byte[100];
        var result = mediaService.uploadImage("photo.webp", "image/webp", 100, new ByteArrayInputStream(content));

        assertNotNull(result.url());
        assertTrue(result.url().endsWith(".webp"));
    }

    @Test
    void uploadWithUnsupportedContentTypeThrows() {
        byte[] content = new byte[100];
        assertThrows(
                InvalidFileException.class,
                () -> mediaService.uploadImage("file.txt", "text/plain", 100, new ByteArrayInputStream(content))
        );
    }

    @Test
    void uploadWithUnsupportedExtensionThrows() {
        byte[] content = new byte[100];
        assertThrows(
                InvalidFileException.class,
                () -> mediaService.uploadImage("file.txt", "image/png", 100, new ByteArrayInputStream(content))
        );
    }

    @Test
    void uploadExceedingSizeLimitThrows() {
        byte[] content = new byte[2048]; // 2KB > 1KB limit
        assertThrows(
                InvalidFileException.class,
                () -> mediaService.uploadImage("photo.png", "image/png", 2048, new ByteArrayInputStream(content))
        );
    }

    @Test
    void uploadWithBlankFilenameThrows() {
        byte[] content = new byte[100];
        assertThrows(
                InvalidFileException.class,
                () -> mediaService.uploadImage("", "image/png", 100, new ByteArrayInputStream(content))
        );
    }

    @Test
    void uploadWithMissingExtensionThrows() {
        byte[] content = new byte[100];
        assertThrows(
                InvalidFileException.class,
                () -> mediaService.uploadImage("noextension", "image/png", 100, new ByteArrayInputStream(content))
        );
    }

    @Test
    void filenamesInExtractsLocalUploadsFromMarkdownAndCover() {
        Set<String> names = LocalMediaReferences.filenamesIn(
                "![a](/uploads/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png) [b](/uploads/bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.jpg)",
                "/uploads/cccccccccccccccccccccccccccccccc.webp"
        );
        assertEquals(Set.of(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.jpg",
                "cccccccccccccccccccccccccccccccc.webp"
        ), names);
    }

    @Test
    void deleteUnreferencedLocalFilesKeepsImagesStillUsedByOtherPosts() {
        InMemoryFileStorageService storage = (InMemoryFileStorageService) fileStorageService;
        var uploaded = mediaService.uploadImage("photo.png", "image/png", 100, new ByteArrayInputStream(new byte[100]));
        Instant now = Instant.parse("2026-08-11T10:00:00Z");
        Post deleted = Post.createDraft(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "A",
                "a",
                "![x](" + uploaded.url() + ")",
                null,
                List.of(),
                now
        );
        Post remaining = Post.createDraft(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "B",
                "b",
                "cover only",
                null,
                List.of(),
                now,
                null,
                uploaded.url()
        );

        mediaService.deleteUnreferencedLocalFiles(deleted, List.of(remaining));

        assertTrue(storage.contains(uploaded.storedFilename()));
    }

    @Test
    void deleteUnreferencedLocalFilesRemovesImagesOnlyUsedByDeletedPost() {
        InMemoryFileStorageService storage = (InMemoryFileStorageService) fileStorageService;
        var uploaded = mediaService.uploadImage("photo.png", "image/png", 100, new ByteArrayInputStream(new byte[100]));
        Instant now = Instant.parse("2026-08-11T10:00:00Z");
        Post deleted = Post.createDraft(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "A",
                "a",
                "![x](" + uploaded.url() + ")",
                null,
                List.of(),
                now
        );

        mediaService.deleteUnreferencedLocalFiles(deleted, List.of());

        assertFalse(storage.contains(uploaded.storedFilename()));
    }

    // 简单的内存文件存储，用于单元测试。
    private static class InMemoryFileStorageService implements FileStorageService {
        private final Set<String> stored = new HashSet<>();

        @Override
        public String store(String storedFilename, java.io.InputStream content) {
            stored.add(storedFilename);
            return "/uploads/" + storedFilename;
        }

        @Override
        public void delete(String storedFilename) {
            stored.remove(storedFilename);
        }

        boolean contains(String storedFilename) {
            return stored.contains(storedFilename);
        }
    }
}
