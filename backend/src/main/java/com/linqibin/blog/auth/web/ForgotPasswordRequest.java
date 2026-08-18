package com.linqibin.blog.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @Email(message = "邮箱格式不正确") @NotBlank(message = "邮箱不能为空") String email
) {
}
