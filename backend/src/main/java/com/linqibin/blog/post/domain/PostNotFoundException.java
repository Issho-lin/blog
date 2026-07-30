package com.linqibin.blog.post.domain;

import java.util.UUID;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(UUID postId) {
        super("文章不存在: " + postId);
    }

    public PostNotFoundException(String slug) {
        super("文章不存在: " + slug);
    }
}
