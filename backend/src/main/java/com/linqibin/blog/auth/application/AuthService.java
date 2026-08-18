package com.linqibin.blog.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.linqibin.blog.auth.domain.User;
import com.linqibin.blog.auth.domain.UserRepository;
import com.linqibin.blog.auth.exception.InvalidCredentialsException;
import com.linqibin.blog.auth.security.LoginAttemptService;

// 认证应用层：负责登录验证、退出清理和当前用户查询的流程串联。
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock,
                        LoginAttemptService loginAttemptService) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.clock = Objects.requireNonNull(clock);
        this.loginAttemptService = Objects.requireNonNull(loginAttemptService);
    }

    // 登录：按邮箱查找用户，用 BCrypt 校验密码，成功后更新最近登录时间。
    public User login(String email, String rawPassword) {
        // 先检查是否因失败过多被锁定。
        if (loginAttemptService.isLocked(email)) {
            log.warn("登录被拒绝（账号已锁定）：email={}", email);
            throw new InvalidCredentialsException("邮箱或密码错误");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(email);
                    return new InvalidCredentialsException("邮箱或密码错误");
                });

        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            loginAttemptService.recordFailure(email);
            throw new InvalidCredentialsException("邮箱或密码错误");
        }

        // 登录成功后清除失败记录并更新最近登录时间。
        loginAttemptService.recordSuccess(email);
        Instant now = Instant.now(clock);
        User updatedUser = user.recordLogin(now);
        return userRepository.save(updatedUser);
    }

    // 初始化默认管理员：在仓库为空时创建第一个用户。
    public User initializeDefaultAdmin(String email, String rawPassword, String displayName) {
        if (!userRepository.isEmpty()) {
            // 已有用户则跳过，避免重复初始化。
            return null;
        }

        Instant now = Instant.now(clock);
        String passwordHash = passwordEncoder.encode(rawPassword);
        User admin = User.create(UUID.randomUUID(), email, passwordHash, displayName, now);
        return userRepository.save(admin);
    }

    // 按邮箱查找用户，供 SecurityContext 恢复时使用。
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("用户不存在"));
    }

    public User changePassword(User principal, String currentPassword, String newPassword) {
        if (principal == null) {
            throw new InvalidCredentialsException("未登录");
        }
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new InvalidCredentialsException("未登录"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new InvalidCredentialsException("当前密码不正确");
        }
        AuthPasswords.assertStrong(newPassword);
        if (passwordEncoder.matches(newPassword, user.passwordHash())) {
            throw new InvalidCredentialsException("新密码不能与当前密码相同");
        }
        Instant now = Instant.now(clock);
        User updated = user.withPasswordHash(passwordEncoder.encode(newPassword), now);
        return userRepository.save(updated);
    }
}
