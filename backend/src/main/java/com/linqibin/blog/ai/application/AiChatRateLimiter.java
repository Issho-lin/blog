package com.linqibin.blog.ai.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.linqibin.blog.ai.domain.AiSettings;
import com.linqibin.blog.ai.exception.AiRateLimitedException;

@Component
public class AiChatRateLimiter {

    private final Clock clock;
    private final AiSettingsService aiSettingsService;
    private final ConcurrentHashMap<String, Deque<Long>> minuteStamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Long>> dayStamps = new ConcurrentHashMap<>();

    public AiChatRateLimiter(Clock clock, AiSettingsService aiSettingsService) {
        this.clock = Objects.requireNonNull(clock);
        this.aiSettingsService = Objects.requireNonNull(aiSettingsService);
    }

    public void assertAllowed(String key) {
        AiSettings settings = aiSettingsService.get();
        String normalized = key == null || key.isBlank() ? "unknown" : key.trim();
        long now = Instant.now(clock).toEpochMilli();
        consume(minuteStamps, normalized, now, settings.ratePerMinute() * 1L, 60_000L);
        consume(dayStamps, normalized, now, settings.ratePerDay() * 1L, 24 * 60 * 60_000L);
    }

    private static void consume(
            ConcurrentHashMap<String, Deque<Long>> stamps,
            String key,
            long now,
            long max,
            long windowMillis
    ) {
        Deque<Long> queue = stamps.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > windowMillis) {
                queue.pollFirst();
            }
            if (queue.size() >= max) {
                throw new AiRateLimitedException();
            }
            queue.addLast(now);
        }
    }
}
