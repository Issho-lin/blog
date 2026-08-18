package com.linqibin.blog.auth.infrastructure;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.linqibin.blog.auth.application.AuthService;
import com.linqibin.blog.auth.application.PasswordResetNotifier;
import com.linqibin.blog.auth.application.PasswordResetService;
import com.linqibin.blog.auth.domain.PasswordResetTokenRepository;
import com.linqibin.blog.auth.domain.UserRepository;
import com.linqibin.blog.auth.infrastructure.persistence.PasswordResetTokenEntityMapper;
import com.linqibin.blog.auth.infrastructure.persistence.PasswordResetTokenRepositoryAdapter;
import com.linqibin.blog.auth.infrastructure.persistence.SpringDataPasswordResetTokenRepository;
import com.linqibin.blog.auth.infrastructure.persistence.SpringDataUserRepository;
import com.linqibin.blog.auth.infrastructure.persistence.UserEntityMapper;
import com.linqibin.blog.auth.infrastructure.persistence.UserRepositoryAdapter;
import com.linqibin.blog.auth.security.LoginAttemptService;

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
        return new InMemoryUserRepository();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "blog.auth",
            name = "repository-type",
            havingValue = "in-memory",
            matchIfMissing = true
    )
    public InMemoryPasswordResetTokenRepository inMemoryPasswordResetTokenRepository() {
        return new InMemoryPasswordResetTokenRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.auth", name = "repository-type", havingValue = "jpa")
    public UserEntityMapper userEntityMapper() {
        return new UserEntityMapper();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.auth", name = "repository-type", havingValue = "jpa")
    public UserRepository jpaUserRepositoryAdapter(
            SpringDataUserRepository springDataUserRepository,
            UserEntityMapper userEntityMapper
    ) {
        return new UserRepositoryAdapter(springDataUserRepository, userEntityMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.auth", name = "repository-type", havingValue = "jpa")
    public PasswordResetTokenEntityMapper passwordResetTokenEntityMapper() {
        return new PasswordResetTokenEntityMapper();
    }

    @Bean
    @ConditionalOnProperty(prefix = "blog.auth", name = "repository-type", havingValue = "jpa")
    public PasswordResetTokenRepository jpaPasswordResetTokenRepositoryAdapter(
            SpringDataPasswordResetTokenRepository springDataPasswordResetTokenRepository,
            PasswordResetTokenEntityMapper passwordResetTokenEntityMapper
    ) {
        return new PasswordResetTokenRepositoryAdapter(
                springDataPasswordResetTokenRepository,
                passwordResetTokenEntityMapper
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public LoggingPasswordResetNotifier passwordResetNotifier() {
        return new LoggingPasswordResetNotifier();
    }

    @Bean
    public AuthService authService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   LoginAttemptService loginAttemptService, Clock clock) {
        return new AuthService(userRepository, passwordEncoder, clock, loginAttemptService);
    }

    @Bean
    public PasswordResetService passwordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetNotifier notifier,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${blog.auth.reset-token-ttl-minutes:120}") long ttlMinutes,
            @Value("${blog.auth.public-site-url:http://localhost:3000}") String publicSiteUrl
    ) {
        return new PasswordResetService(
                userRepository,
                tokenRepository,
                notifier,
                passwordEncoder,
                clock,
                Duration.ofMinutes(ttlMinutes),
                publicSiteUrl
        );
    }
}
