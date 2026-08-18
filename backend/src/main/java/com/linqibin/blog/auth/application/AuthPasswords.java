package com.linqibin.blog.auth.application;

import com.linqibin.blog.auth.exception.WeakPasswordException;

public final class AuthPasswords {

    private AuthPasswords() {
    }

    public static void assertStrong(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new WeakPasswordException("新密码不能为空");
        }
        if (rawPassword.length() < 8) {
            throw new WeakPasswordException("新密码至少 8 个字符");
        }
        if (rawPassword.length() > 72) {
            throw new WeakPasswordException("新密码不能超过 72 个字符");
        }
    }
}
