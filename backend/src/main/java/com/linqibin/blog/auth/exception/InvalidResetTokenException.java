package com.linqibin.blog.auth.exception;

public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("重置链接无效或已过期");
    }
}
