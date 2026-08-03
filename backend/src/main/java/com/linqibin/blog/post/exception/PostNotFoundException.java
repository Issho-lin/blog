package com.linqibin.blog.post.exception;

import java.util.UUID;

// 文章不存在异常：按 id 或 slug 查询不到文章时统一抛这个异常。
public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(UUID postId) {
        super("文章不存在: " + postId);
    }

    public PostNotFoundException(String slug) {
        super("文章不存在: " + slug);
    }
}
