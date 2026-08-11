package com.linqibin.blog.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 登录请求：只需邮箱和密码，密码以明文传输由 HTTPS 保护，服务端用 BCrypt 校验。
public record LoginRequest(
        @Email(message = "邮箱格式不正确") @NotBlank(message = "邮箱不能为空") String email,
        @NotBlank(message = "密码不能为空") String password
) {
}
