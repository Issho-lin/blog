package com.linqibin.blog.post.exception;

public class DuplicateSlugException extends RuntimeException {

    public DuplicateSlugException(String slug) {
        super("slug 已存在: " + slug);
    }
}
