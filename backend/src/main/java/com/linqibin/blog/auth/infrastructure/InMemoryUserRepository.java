package com.linqibin.blog.auth.infrastructure;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.linqibin.blog.auth.domain.User;
import com.linqibin.blog.auth.domain.UserRepository;

// 内存版用户仓库：用于开发和测试阶段，后续可以替换成数据库实现。
public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();

    public void clear() {
        // 测试前清空内存数据，避免用例互相污染。
        users.clear();
    }

    @Override
    public User save(User user) {
        // 用用户 id 覆盖保存，既支持创建也支持更新。
        users.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        // 登录时按邮箱查找用户，遍历实现后续切数据库再优化。
        return users.values().stream()
                .filter(user -> user.email().equals(email))
                .findFirst();
    }

    @Override
    public boolean isEmpty() {
        return users.isEmpty();
    }
}
