package com.linqibin.blog.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data JPA 仓库：直接面向 UserEntity 提供查表能力。
public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    // 登录时按邮箱查找用户。
    Optional<UserEntity> findByEmail(String email);
}
