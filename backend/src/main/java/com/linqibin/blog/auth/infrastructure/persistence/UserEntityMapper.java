package com.linqibin.blog.auth.infrastructure.persistence;

import com.linqibin.blog.auth.domain.User;

// 专职负责领域对象与持久化实体之间的双向翻译，避免转换逻辑散落在适配器里。
public class UserEntityMapper {

    public UserEntity toEntity(User user) {
        return new UserEntity(
                user.id(),
                user.email(),
                user.passwordHash(),
                user.displayName(),
                user.role(),
                user.createdAt(),
                user.updatedAt(),
                user.lastLoginAt()
        );
    }

    public User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                entity.getRole(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastLoginAt()
        );
    }
}
