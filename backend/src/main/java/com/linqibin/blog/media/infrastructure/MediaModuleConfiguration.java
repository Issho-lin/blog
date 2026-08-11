package com.linqibin.blog.media.infrastructure;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.linqibin.blog.media.application.MediaService;

// 媒体模块配置：注册文件存储服务和上传服务 Bean。
@Configuration
public class MediaModuleConfiguration {

    @Bean
    public FileStorageService fileStorageService(
            @Value("${blog.media.upload-dir:./uploads}") String uploadDir,
            @Value("${blog.media.url-prefix:/uploads/}") String urlPrefix
    ) {
        return new LocalFileStorageService(uploadDir, urlPrefix);
    }

    @Bean
    public MediaService mediaService(
            FileStorageService fileStorageService,
            Clock clock,
            @Value("${blog.media.max-image-size:10485760}") long maxImageSize
    ) {
        return new MediaService(fileStorageService, clock, maxImageSize);
    }
}
