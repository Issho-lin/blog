package com.linqibin.blog.auth.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.linqibin.blog.auth.application.AuthService;
import com.linqibin.blog.auth.domain.UserRepository;
import com.linqibin.blog.auth.infrastructure.persistence.UserEntityMapper;
import com.linqibin.blog.auth.infrastructure.persistence.UserRepositoryAdapter;
import com.linqibin.blog.auth.infrastructure.persistence.SpringDataUserRepository;
import com.linqibin.blog.auth.security.LoginAttemptService;

// 统一注册 auth 模块需要的 Spring Bean，方便 web 层和安全层直接注入使用。
@Configuration
public class UserModuleConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "blog.auth",
            name = "repository-type",
            havingValue = "in-memory",
            matchIfMissing = true
    )
    public InMemoryUserRepository inMemoryUserRepository() {
        // 默认走内存仓库，保证本地开发和测试无需数据库即可运行。
        return new InMemoryUserRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.auth", name = "repository-type", havingValue = "jpa")
    public UserEntityMapper userEntityMapper() {
        // JPA 模式下显式注册翻译器，把领域对象和数据库实体隔离开。
        return new UserEntityMapper();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.auth", name = "repository-type", havingValue = "jpa")
    public UserRepository jpaUserRepositoryAdapter(
            SpringDataUserRepository springDataUserRepository,
            UserEntityMapper userEntityMapper
    ) {
        // JPA 模式下改由适配器承接领域仓库接口，上层仍只依赖 UserRepository。
        return new UserRepositoryAdapter(springDataUserRepository, userEntityMapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 自带盐值，每次加密结果不同，是目前推荐的密码哈希方案。
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthService authService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   LoginAttemptService loginAttemptService, java.time.Clock clock) {
        // AuthService 依赖抽象仓库、密码编码器和登录限流服务，后续切数据库时只需要替换仓库实现。
        return new AuthService(userRepository, passwordEncoder, clock, loginAttemptService);
    }
}
