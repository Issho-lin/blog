package com.linqibin.blog.media.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.linqibin.blog.media.exception.FileStorageException;
import com.linqibin.blog.media.infrastructure.S3FileStorageService;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicMediaControllerTest {

    @Mock
    private S3FileStorageService s3FileStorageService;

    private PublicMediaController controller;

    @BeforeEach
    void setUp() {
        controller = new PublicMediaController(s3FileStorageService);
    }

    @Test
    void returnsObjectBytesForSafeFilename() {
        byte[] body = {9, 8, 7};
        when(s3FileStorageService.read("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png")).thenReturn(body);
        when(s3FileStorageService.contentTypeOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png")).thenReturn("image/png");

        ResponseEntity<byte[]> response = controller.get("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(body, response.getBody());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
    }

    @Test
    void rejectsUnsafeFilename() {
        ResponseEntity<byte[]> response = controller.get("../secret.png");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(s3FileStorageService, never()).read("../secret.png");
    }

    @Test
    void missingObjectReturnsNotFound() {
        when(s3FileStorageService.read("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.jpg"))
                .thenThrow(new FileStorageException("文件不存在", null));

        ResponseEntity<byte[]> response = controller.get("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.jpg");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
