package com.linqibin.blog.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Web MVC 配置：注册静态资源处理器，让上传的图片可以通过 URL 直接访问。
// 资源保存路径与公开 URL 解耦：URL 前缀映射到本地存储目录。
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Value("${blog.media.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${blog.media.url-prefix:/uploads/}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把 /uploads/** 映射到本地文件系统目录，让上传的图片可以公开访问。
        String location = uploadDir.startsWith("/") || uploadDir.startsWith("file:")
                ? uploadDir.endsWith("/") ? "file:" + uploadDir : "file:" + uploadDir + "/"
                : "file:" + System.getProperty("user.dir") + "/" + normalizePath(uploadDir);

        registry.addResourceHandler(urlPrefix.endsWith("/") ? urlPrefix + "**" : urlPrefix + "/**")
                .addResourceLocations(location);
    }

    private String normalizePath(String path) {
        String normalized = path.startsWith("./") ? path.substring(2) : path;
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }
}
