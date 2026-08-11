package com.linqibin.blog.media.exception;

// 文件存储失败异常：磁盘写入、目录创建等底层 IO 操作失败时抛出。
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
