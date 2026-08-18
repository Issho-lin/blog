package com.linqibin.blog.comment.exception;

public class CommentRateLimitedException extends RuntimeException {

    public CommentRateLimitedException() {
        super("评论过于频繁，请稍后再试");
    }
}
