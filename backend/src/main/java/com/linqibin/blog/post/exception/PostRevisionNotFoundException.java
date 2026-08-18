package com.linqibin.blog.post.exception;

import java.util.UUID;

public class PostRevisionNotFoundException extends RuntimeException {

    public PostRevisionNotFoundException(UUID id) {
        super("版本不存在");
    }
}
