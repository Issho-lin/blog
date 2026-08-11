package com.linqibin.blog.media.domain;

import java.time.Instant;
import java.util.UUID;

// 上传媒体文件的领域模型：记录文件的基本信息和存储位置。
public record MediaFile(
        UUID id,
        String originalFilename,
        String storedFilename,
        String contentType,
        long size,
        String url,
        Instant uploadedAt
) {
}
