package com.linqibin.blog.media.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import com.linqibin.blog.media.exception.FileStorageException;

// 本地文件系统存储实现：文件保存到配置的目录，通过 URL 前缀对外暴露。
// 存储路径与公开 URL 解耦：文件名使用 UUID 防止路径遍历攻击。
public class LocalFileStorageService implements FileStorageService {

    private final Path storageDir;
    private final String urlPrefix;

    public LocalFileStorageService(String storageDir, String urlPrefix) {
        this.storageDir = Paths.get(Objects.requireNonNull(storageDir));
        this.urlPrefix = normalizeUrlPrefix(urlPrefix);
        ensureDirectoryExists();
    }

    @Override
    public String store(String storedFilename, InputStream content) {
        try {
            Path target = storageDir.resolve(storedFilename);
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return urlPrefix + storedFilename;
        } catch (IOException e) {
            throw new FileStorageException("文件存储失败: " + storedFilename, e);
        }
    }

    @Override
    public void delete(String storedFilename) {
        try {
            Files.deleteIfExists(storageDir.resolve(storedFilename));
        } catch (IOException e) {
            throw new FileStorageException("文件删除失败: " + storedFilename, e);
        }
    }

    Path getStorageDir() {
        return storageDir;
    }

    String getUrlPrefix() {
        return urlPrefix;
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new FileStorageException("存储目录创建失败: " + storageDir, e);
        }
    }

    private String normalizeUrlPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "/uploads/";
        }
        String normalized = prefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }
}
