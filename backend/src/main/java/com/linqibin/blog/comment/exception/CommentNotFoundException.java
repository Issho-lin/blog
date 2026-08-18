package com.linqibin.blog.comment.exception;

import java.util.UUID;

public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(UUID id) {
        super("评论不存在");
    }
}
