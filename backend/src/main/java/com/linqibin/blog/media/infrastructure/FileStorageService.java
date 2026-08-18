package com.linqibin.blog.media.infrastructure;

import java.io.InputStream;

// 文件存储抽象：将文件保存到存储后端并返回可访问的 URL。
// 实现可以是本地目录或 S3 兼容对象存储（MinIO）。
public interface FileStorageService {

    // 存储文件并返回可访问的 URL。
    String store(String storedFilename, InputStream content);

    // 删除文件。
    void delete(String storedFilename);
}
