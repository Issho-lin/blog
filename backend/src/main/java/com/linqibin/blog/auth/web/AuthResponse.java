package com.linqibin.blog.auth.web;

import java.time.Instant;
import java.util.UUID;

import com.linqibin.blog.auth.domain.User;
import com.linqibin.blog.auth.domain.UserRole;

// 认证响应：返回用户基本信息，绝不包含密码哈希。
public record AuthResponse(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        Instant lastLoginAt
) {
    public static AuthResponse from(User user) {
        return new AuthResponse(
                user.id(),
                user.email(),
                user.displayName(),
                user.role(),
                user.lastLoginAt()
        );
    }
}
