package com.linqibin.blog.ai.exception;

public class AiRateLimitedException extends RuntimeException {

    public AiRateLimitedException() {
        super("提问过于频繁，请稍后再试");
    }
}
