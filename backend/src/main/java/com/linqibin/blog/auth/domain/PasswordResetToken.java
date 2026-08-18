package com.linqibin.blog.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PasswordResetToken(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant expiresAt,
        Instant usedAt
) {

    public PasswordResetToken {
        Objects.requireNonNull(id, "id 不能为空");
        Objects.requireNonNull(userId, "用户 id 不能为空");
        Objects.requireNonNull(tokenHash, "令牌哈希不能为空");
        Objects.requireNonNull(expiresAt, "过期时间不能为空");
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public PasswordResetToken markUsed(Instant now) {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, now);
    }
}
