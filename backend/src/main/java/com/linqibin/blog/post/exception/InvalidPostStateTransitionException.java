package com.linqibin.blog.post.exception;

import com.linqibin.blog.post.domain.PostStatus;

// 状态流转异常：当文章当前状态不支持目标操作时抛出。
public class InvalidPostStateTransitionException extends RuntimeException {

    public InvalidPostStateTransitionException(PostStatus from, String action) {
        super("文章状态 " + from + " 不允许执行操作: " + action);
    }
}
