package com.linqibin.blog.media.application;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.linqibin.blog.media.domain.MediaFile;
import com.linqibin.blog.media.exception.InvalidFileException;
import com.linqibin.blog.media.infrastructure.FileStorageService;

// 媒体上传服务：校验文件类型和大小，生成安全文件名，调用存储服务保存。
// 校验不只依赖扩展名，还检查 Content-Type，防止伪装文件。
public class MediaService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    private static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024; // 10 MB

    private final FileStorageService fileStorageService;
    private final Clock clock;
    private final long maxFileSize;

    public MediaService(FileStorageService fileStorageService, Clock clock) {
        this(fileStorageService, clock, DEFAULT_MAX_SIZE);
    }

    public MediaService(FileStorageService fileStorageService, Clock clock, long maxFileSize) {
        this.fileStorageService = Objects.requireNonNull(fileStorageService);
        this.clock = Objects.requireNonNull(clock);
        this.maxFileSize = maxFileSize;
    }

    // 上传图片文件，返回包含可访问 URL 的 MediaFile。
    public MediaFile uploadImage(String originalFilename, String contentType, long size, InputStream content) {
        validateFile(originalFilename, contentType, size);
        String extension = extractExtension(originalFilename);
        String storedFilename = generateStoredFilename(extension);
        String url = fileStorageService.store(storedFilename, content);
        Instant now = Instant.now(clock);
        return new MediaFile(
                UUID.randomUUID(),
                originalFilename,
                storedFilename,
                contentType,
                size,
                url,
                now
        );
    }

    private void validateFile(String filename, String contentType, long size) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileException("文件名不能为空");
        }
        if (size > maxFileSize) {
            throw new InvalidFileException("文件大小超过限制: " + size + " bytes, 上限 " + maxFileSize + " bytes");
        }
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException("不支持的文件类型: " + contentType);
        }
        String extension = extractExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException("不支持的文件扩展名: " + extension);
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new InvalidFileException("文件缺少有效扩展名");
        }
        return filename.substring(dotIndex);
    }

    // 使用 UUID 生成存储文件名，避免路径遍历和文件名冲突。
    private String generateStoredFilename(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + extension;
    }
}
