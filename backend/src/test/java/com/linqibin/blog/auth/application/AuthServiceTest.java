package com.linqibin.blog.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.linqibin.blog.auth.domain.User;
import com.linqibin.blog.auth.exception.InvalidCredentialsException;
import com.linqibin.blog.auth.infrastructure.InMemoryUserRepository;
import com.linqibin.blog.auth.security.LoginAttemptService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private InMemoryUserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private Clock clock;
    private LoginAttemptService loginAttemptService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        this.userRepository = new InMemoryUserRepository();
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.clock = Clock.fixed(Instant.parse("2026-08-10T10:00:00Z"), ZoneOffset.UTC);
        this.loginAttemptService = new LoginAttemptService(clock, 5, 15);
        this.authService = new AuthService(userRepository, passwordEncoder, clock, loginAttemptService);
    }

    @Test
    void loginWithValidCredentialsReturnsUser() {
        // 预置一个用户，密码哈希后存入仓库。
        String rawPassword = "password123";
        String hash = passwordEncoder.encode(rawPassword);
        User user = User.create(
                java.util.UUID.randomUUID(), "admin@blog.com", hash, "Admin",
                Instant.parse("2026-08-01T00:00:00Z")
        );
        userRepository.save(user);

        User result = authService.login("admin@blog.com", rawPassword);

        assertEquals(user.id(), result.id());
        assertEquals(user.email(), result.email());
        // 登录后 lastLoginAt 应被更新。
        assertNotNull(result.lastLoginAt());
        assertEquals(Instant.now(clock), result.lastLoginAt());
    }

    @Test
    void loginWithInvalidPasswordThrowsException() {
        String hash = passwordEncoder.encode("correct-password");
        User user = User.create(
                java.util.UUID.randomUUID(), "admin@blog.com", hash, "Admin",
                Instant.parse("2026-08-01T00:00:00Z")
        );
        userRepository.save(user);

        assertThrows(InvalidCredentialsException.class, () ->
                authService.login("admin@blog.com", "wrong-password"));
    }

    @Test
    void loginWithNonExistentEmailThrowsException() {
        assertThrows(InvalidCredentialsException.class, () ->
                authService.login("nobody@blog.com", "password"));
    }

    @Test
    void loginUpdatesLastLoginAt() {
        String hash = passwordEncoder.encode("password123");
        User user = User.create(
                java.util.UUID.randomUUID(), "admin@blog.com", hash, "Admin",
                Instant.parse("2026-08-01T00:00:00Z")
        );
        userRepository.save(user);
        assertNull(user.lastLoginAt());

        User result = authService.login("admin@blog.com", "password123");

        assertNotNull(result.lastLoginAt());
        // 仓库中保存的也应该是更新后的。
        User saved = userRepository.findByEmail("admin@blog.com").orElseThrow();
        assertEquals(result.lastLoginAt(), saved.lastLoginAt());
    }

    @Test
    void loginErrorMessageDoesNotLeakWhichFieldIsWrong() {
        // 无论邮箱不存在还是密码错误，都返回相同消息，防止枚举攻击。
        String hash = passwordEncoder.encode("password123");
        User user = User.create(
                java.util.UUID.randomUUID(), "admin@blog.com", hash, "Admin",
                Instant.parse("2026-08-01T00:00:00Z")
        );
        userRepository.save(user);

        InvalidCredentialsException emailNotFound = assertThrows(InvalidCredentialsException.class, () ->
                authService.login("nobody@blog.com", "password123"));
        InvalidCredentialsException wrongPassword = assertThrows(InvalidCredentialsException.class, () ->
                authService.login("admin@blog.com", "wrong-password"));

        assertEquals(emailNotFound.getMessage(), wrongPassword.getMessage());
    }

    @Test
    void initializeDefaultAdminCreatesUserWhenRepositoryEmpty() {
        assertNull(userRepository.findByEmail("admin@blog.com").orElse(null));

        User admin = authService.initializeDefaultAdmin("admin@blog.com", "admin123", "Admin");

        assertNotNull(admin);
        assertEquals("admin@blog.com", admin.email());
        assertEquals("Admin", admin.displayName());
        // 密码应以哈希存储，且与明文不同。
        assertNotEquals("admin123", admin.passwordHash());
        assertTrue(passwordEncoder.matches("admin123", admin.passwordHash()));
    }

    @Test
    void initializeDefaultAdminSkipsWhenRepositoryNotEmpty() {
        // 预置一个用户。
        authService.initializeDefaultAdmin("admin@blog.com", "admin123", "Admin");
        assertTrue(userRepository.findByEmail("admin@blog.com").isPresent());

        // 再次初始化应跳过。
        User result = authService.initializeDefaultAdmin("new@blog.com", "newpass", "New");
        assertNull(result);
        // 不应创建第二个用户。
        assertTrue(userRepository.findByEmail("new@blog.com").isEmpty());
    }

    @Test
    void findByEmailReturnsUserWhenExists() {
        authService.initializeDefaultAdmin("admin@blog.com", "admin123", "Admin");

        User found = authService.findByEmail("admin@blog.com");

        assertEquals("admin@blog.com", found.email());
    }

    @Test
    void findByEmailThrowsWhenNotFound() {
        assertThrows(InvalidCredentialsException.class, () ->
                authService.findByEmail("nobody@blog.com"));
    }

    @Test
    void loginLocksAfterMaxFailedAttempts() {
        String hash = passwordEncoder.encode("password123");
        User user = User.create(
                java.util.UUID.randomUUID(), "admin@blog.com", hash, "Admin",
                Instant.parse("2026-08-01T00:00:00Z")
        );
        userRepository.save(user);

        // 连续失败 5 次。
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidCredentialsException.class, () ->
                    authService.login("admin@blog.com", "wrong-password"));
        }

        // 第 6 次即使密码正确也会被拒绝。
        assertThrows(InvalidCredentialsException.class, () ->
                authService.login("admin@blog.com", "password123"));
    }

    @Test
    void loginSuccessClearsFailureCount() {
        String hash = passwordEncoder.encode("password123");
        User user = User.create(
                java.util.UUID.randomUUID(), "admin@blog.com", hash, "Admin",
                Instant.parse("2026-08-01T00:00:00Z")
        );
        userRepository.save(user);

        // 失败 3 次。
        for (int i = 0; i < 3; i++) {
            assertThrows(InvalidCredentialsException.class, () ->
                    authService.login("admin@blog.com", "wrong-password"));
        }

        // 正确密码登录成功，清除失败记录。
        User result = authService.login("admin@blog.com", "password123");
        assertEquals("admin@blog.com", result.email());

        // 再次失败 3 次不应被锁定（计数已重置）。
        for (int i = 0; i < 3; i++) {
            assertThrows(InvalidCredentialsException.class, () ->
                    authService.login("admin@blog.com", "wrong-password"));
        }
        // 第 4 次仍可用正确密码登录。
        User result2 = authService.login("admin@blog.com", "password123");
        assertEquals("admin@blog.com", result2.email());
    }
}
