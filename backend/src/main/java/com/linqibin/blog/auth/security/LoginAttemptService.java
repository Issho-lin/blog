package com.linqibin.blog.auth.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 登录失败限流：按邮箱追踪失败次数，超过阈值后临时锁定，防止暴力枚举密码。
// 首期使用内存实现，后续可替换为 Redis 版本以支持多实例部署。
@Component
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final Clock clock;
    private final int maxAttempts;
    private final long lockDurationMillis;

    // 记录每个邮箱的失败次数和锁定时间。
    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            Clock clock,
            @Value("${blog.auth.max-login-attempts:5}") int maxAttempts,
            @Value("${blog.auth.lock-duration-minutes:15}") long lockDurationMinutes
    ) {
        this.clock = Objects.requireNonNull(clock);
        this.maxAttempts = maxAttempts;
        this.lockDurationMillis = lockDurationMinutes * 60_000;
    }

    // 检查指定邮箱是否已被锁定。
    public boolean isLocked(String email) {
        AttemptRecord record = attempts.get(email);
        if (record == null) {
            return false;
        }
        if (record.lockedUntil == null) {
            return false;
        }
        boolean locked = Instant.now(clock).isBefore(record.lockedUntil);
        if (!locked) {
            // 锁定已过期，清除记录。
            attempts.remove(email);
        }
        return locked;
    }

    // 记录一次失败尝试，达到阈值后锁定。
    public void recordFailure(String email) {
        AttemptRecord record = attempts.computeIfAbsent(email, k -> new AttemptRecord());
        int count = record.count.incrementAndGet();
        log.warn("登录失败：email={}, 失败次数={}/{}, requestId 已由过滤器记录", email, count, maxAttempts);

        if (count >= maxAttempts) {
            record.lockedUntil = Instant.now(clock).plusMillis(lockDurationMillis);
            log.warn("账号已锁定：email={}, 锁定时长={}ms", email, lockDurationMillis);
        }
    }

    // 登录成功后清除失败记录。
    public void recordSuccess(String email) {
        attempts.remove(email);
        log.info("登录成功：email={}", email);
    }
}

// 内部记录类：追踪失败次数和锁定截止时间。
class AttemptRecord {
    final AtomicInteger count = new AtomicInteger(0);
    volatile Instant lockedUntil;
}
