package com.linqibin.blog.media.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.linqibin.blog.media.application.MediaService;
import com.linqibin.blog.media.domain.MediaFile;

import java.io.IOException;

// 管理端媒体上传接口：作者上传图片后获得可访问 URL。
@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {

    private final MediaService mediaService;

    public AdminMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/images")
    public ResponseEntity<MediaUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        MediaFile mediaFile = mediaService.uploadImage(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(MediaUploadResponse.from(mediaFile));
    }
}
