package com.linqibin.blog.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.linqibin.blog.auth.domain.User;
import com.linqibin.blog.auth.domain.UserRepository;

// 数据库版用户仓库适配器：对外实现领域仓库接口，对内委托 Spring Data JPA。
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserEntityMapper userEntityMapper;

    public UserRepositoryAdapter(
            SpringDataUserRepository springDataUserRepository,
            UserEntityMapper userEntityMapper
    ) {
        this.springDataUserRepository = springDataUserRepository;
        this.userEntityMapper = userEntityMapper;
    }

    @Override
    public User save(User user) {
        UserEntity savedEntity = springDataUserRepository.save(userEntityMapper.toEntity(user));
        return userEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(id)
                .map(userEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email)
                .map(userEntityMapper::toDomain);
    }

    @Override
    public boolean isEmpty() {
        return springDataUserRepository.count() == 0;
    }
}
