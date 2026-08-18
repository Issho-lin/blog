package com.linqibin.blog.auth.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.linqibin.blog.auth.domain.User;
import com.linqibin.blog.auth.exception.InvalidCredentialsException;
import com.linqibin.blog.auth.exception.InvalidResetTokenException;
import com.linqibin.blog.auth.infrastructure.InMemoryPasswordResetTokenRepository;
import com.linqibin.blog.auth.infrastructure.InMemoryUserRepository;
import com.linqibin.blog.auth.security.LoginAttemptService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetServiceTest {

    private InMemoryUserRepository userRepository;
    private InMemoryPasswordResetTokenRepository tokenRepository;
    private PasswordEncoder passwordEncoder;
    private Clock clock;
    private AuthService authService;
    private CapturingNotifier notifier;
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        tokenRepository = new InMemoryPasswordResetTokenRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);
        authService = new AuthService(userRepository, passwordEncoder, clock, new LoginAttemptService(clock, 5, 15));
        notifier = new CapturingNotifier();
        passwordResetService = new PasswordResetService(
                userRepository,
                tokenRepository,
                notifier,
                passwordEncoder,
                clock,
                Duration.ofMinutes(120),
                "http://localhost:3000"
        );
        authService.initializeDefaultAdmin("admin@blog.com", "admin1234", "Admin");
    }

    @Test
    void changePasswordRequiresCurrentPassword() {
        User user = userRepository.findByEmail("admin@blog.com").orElseThrow();
        assertThrows(InvalidCredentialsException.class, () ->
                authService.changePassword(user, "wrong", "newpassword"));
        authService.changePassword(user, "admin1234", "newpassword");
        assertTrue(passwordEncoder.matches("newpassword",
                userRepository.findByEmail("admin@blog.com").orElseThrow().passwordHash()));
    }

    @Test
    void forgotPasswordDoesNotRevealMissingEmail() {
        String missing = passwordResetService.requestReset("nobody@blog.com");
        String existing = passwordResetService.requestReset("admin@blog.com");
        assertEquals(missing, existing);
        assertTrue(notifier.url.contains("token="));
    }

    @Test
    void resetPasswordWithTokenUpdatesHash() {
        passwordResetService.requestReset("admin@blog.com");
        String token = notifier.url.substring(notifier.url.indexOf("token=") + 6);
        passwordResetService.resetPassword(token, "brand-new-pass");
        assertTrue(passwordEncoder.matches("brand-new-pass",
                userRepository.findByEmail("admin@blog.com").orElseThrow().passwordHash()));
        assertThrows(InvalidResetTokenException.class, () ->
                passwordResetService.resetPassword(token, "another-password"));
    }

    private static final class CapturingNotifier implements PasswordResetNotifier {
        private String url;

        @Override
        public void sendResetLink(String email, String resetUrl) {
            this.url = resetUrl;
        }
    }
}
