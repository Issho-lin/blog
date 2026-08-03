package com.linqibin.blog.post.exception;

// slug 冲突异常：当用户指定的 slug 已被其他文章占用时抛出。
public class DuplicateSlugException extends RuntimeException {

    public DuplicateSlugException(String slug) {
        super("slug 已存在: " + slug);
    }
}
