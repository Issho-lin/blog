package com.linqibin.blog.auth.domain;

import java.util.Optional;
import java.util.UUID;

// 用户仓库抽象：领域层只定义需要的持久化能力。
public interface UserRepository {

    // 保存用户的新状态。
    User save(User user);

    // 按主键查询用户。
    Optional<User> findById(UUID id);

    // 按邮箱查询用户，登录时使用。
    Optional<User> findByEmail(String email);

    // 判断是否已有任何用户存在，用于启动时初始化默认管理员。
    boolean isEmpty();
}
