package com.linqibin.blog.media.web;

import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.linqibin.blog.media.exception.FileStorageException;
import com.linqibin.blog.media.infrastructure.S3FileStorageService;

// 对象存储模式下，公开 URL 仍走 /uploads/{filename}，由后端从 MinIO/S3 读取。
@RestController
@ConditionalOnProperty(prefix = "blog.media", name = "storage-type", havingValue = "s3")
public class PublicMediaController {

    private static final Pattern SAFE_NAME = Pattern.compile(
            "^[0-9a-fA-F]{32}\\.(?:jpe?g|png|gif|webp)$"
    );

    private final S3FileStorageService s3FileStorageService;

    public PublicMediaController(S3FileStorageService s3FileStorageService) {
        this.s3FileStorageService = s3FileStorageService;
    }

    @GetMapping("/uploads/{filename}")
    public ResponseEntity<byte[]> get(@PathVariable String filename) {
        if (!SAFE_NAME.matcher(filename).matches()) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] body = s3FileStorageService.read(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(s3FileStorageService.contentTypeOf(filename)))
                    .contentLength(body.length)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic())
                    .body(body);
        } catch (FileStorageException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
