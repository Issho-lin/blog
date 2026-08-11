package com.linqibin.blog.auth.exception;

// 登录凭据无效时抛出，由全局异常处理器转换为 401 响应。
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
