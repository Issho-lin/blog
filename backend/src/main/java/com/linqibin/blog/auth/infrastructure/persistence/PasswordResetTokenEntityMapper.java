package com.linqibin.blog.auth.infrastructure.persistence;

import com.linqibin.blog.auth.domain.PasswordResetToken;

public class PasswordResetTokenEntityMapper {

    public PasswordResetTokenEntity toEntity(PasswordResetToken token) {
        return new PasswordResetTokenEntity(
                token.id(),
                token.userId(),
                token.tokenHash(),
                token.expiresAt(),
                token.usedAt()
        );
    }

    public PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        return new PasswordResetToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getUsedAt()
        );
    }
}
