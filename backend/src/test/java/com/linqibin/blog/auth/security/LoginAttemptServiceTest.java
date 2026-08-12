package com.linqibin.blog.auth.security;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    private static final String EMAIL = "admin@blog.com";

    private Instant currentTime;
    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        currentTime = Instant.parse("2026-08-11T10:00:00Z");
        Clock clock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return currentTime;
            }
        };
        loginAttemptService = new LoginAttemptService(clock, 3, 10);
    }

    @Test
    void accountIsNotLockedInitially() {
        assertFalse(loginAttemptService.isLocked(EMAIL));
    }

    @Test
    void accountLocksAfterMaxFailures() {
        loginAttemptService.recordFailure(EMAIL);
        loginAttemptService.recordFailure(EMAIL);
        assertFalse(loginAttemptService.isLocked(EMAIL));

        loginAttemptService.recordFailure(EMAIL);

        assertTrue(loginAttemptService.isLocked(EMAIL));
    }

    @Test
    void successClearsFailureRecord() {
        loginAttemptService.recordFailure(EMAIL);
        loginAttemptService.recordFailure(EMAIL);
        loginAttemptService.recordSuccess(EMAIL);

        loginAttemptService.recordFailure(EMAIL);
        loginAttemptService.recordFailure(EMAIL);

        assertFalse(loginAttemptService.isLocked(EMAIL));
    }

    @Test
    void lockExpiresAfterDuration() {
        loginAttemptService.recordFailure(EMAIL);
        loginAttemptService.recordFailure(EMAIL);
        loginAttemptService.recordFailure(EMAIL);
        assertTrue(loginAttemptService.isLocked(EMAIL));

        currentTime = currentTime.plusSeconds(10 * 60 + 1);
        assertFalse(loginAttemptService.isLocked(EMAIL));
    }
}
