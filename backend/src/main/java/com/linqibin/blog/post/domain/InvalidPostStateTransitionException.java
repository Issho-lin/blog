package com.linqibin.blog.post.domain;

public class InvalidPostStateTransitionException extends RuntimeException {

    public InvalidPostStateTransitionException(PostStatus from, String action) {
        super("文章状态 " + from + " 不允许执行操作: " + action);
    }
}
