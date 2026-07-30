package com.linqibin.blog.post.domain;

public class DuplicateSlugException extends RuntimeException {

    public DuplicateSlugException(String slug) {
        super("slug 已存在: " + slug);
    }
}
