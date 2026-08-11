package com.linqibin.blog.media.application;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.linqibin.blog.media.exception.InvalidFileException;
import com.linqibin.blog.media.infrastructure.FileStorageService;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    // 简单的内存文件存储，用于单元测试。
    private static class InMemoryFileStorageService implements FileStorageService {
        @Override
        public String store(String storedFilename, java.io.InputStream content) {
            return "/uploads/" + storedFilename;
        }

        @Override
        public void delete(String storedFilename) {
            // no-op
        }
    }
}
