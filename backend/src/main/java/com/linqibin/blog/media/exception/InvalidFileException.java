package com.linqibin.blog.media.exception;

// 文件校验失败异常：文件类型不允许、大小超限或内容不合法时抛出。
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
