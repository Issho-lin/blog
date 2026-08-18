package com.linqibin.blog.auth.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash,
        String displayName,
        UserRole role,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt
) implements Serializable {

    public User {
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(email, "邮箱不能为空");
        Objects.requireNonNull(passwordHash, "密码哈希不能为空");
        Objects.requireNonNull(displayName, "显示名不能为空");
        Objects.requireNonNull(role, "角色不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        Objects.requireNonNull(updatedAt, "更新时间不能为空");

        email = email.trim();
        displayName = displayName.trim();

        if (email.isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("显示名不能为空");
        }
    }

    // 创建新用户的工厂方法：默认角色为 AUTHOR，登录时间为空。
    public static User create(UUID id, String email, String passwordHash, String displayName, Instant now) {
        return new User(id, email, passwordHash, displayName, UserRole.AUTHOR, now, now, null);
    }

    // 登录成功后更新最近登录时间。
    public User recordLogin(Instant now) {
        return new User(id, email, passwordHash, displayName, role, createdAt, now, now);
    }

    public User withPasswordHash(String passwordHash, Instant now) {
        Objects.requireNonNull(passwordHash, "密码哈希不能为空");
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("密码哈希不能为空");
        }
        return new User(id, email, passwordHash, displayName, role, createdAt, now, lastLoginAt);
    }
}
