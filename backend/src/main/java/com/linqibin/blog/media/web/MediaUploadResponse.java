package com.linqibin.blog.media.web;

import java.time.Instant;
import java.util.UUID;

import com.linqibin.blog.media.domain.MediaFile;

// 图片上传响应：返回稳定 URL 供前端插入 Markdown。
public record MediaUploadResponse(
        UUID id,
        String url,
        String originalFilename,
        String contentType,
        long size,
        Instant uploadedAt
) {

    public static MediaUploadResponse from(MediaFile mediaFile) {
        return new MediaUploadResponse(
                mediaFile.id(),
                mediaFile.url(),
                mediaFile.originalFilename(),
                mediaFile.contentType(),
                mediaFile.size(),
                mediaFile.uploadedAt()
        );
    }
}
