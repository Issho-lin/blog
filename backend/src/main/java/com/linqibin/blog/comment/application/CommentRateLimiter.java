package com.linqibin.blog.comment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.linqibin.blog.comment.exception.CommentRateLimitedException;

@Component
public class CommentRateLimiter {

    private final Clock clock;
    private final int maxPerWindow;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Deque<Long>> stamps = new ConcurrentHashMap<>();

    public CommentRateLimiter(
            Clock clock,
            @Value("${blog.comment.max-per-window:5}") int maxPerWindow,
            @Value("${blog.comment.window-minutes:10}") long windowMinutes
    ) {
        this.clock = Objects.requireNonNull(clock);
        this.maxPerWindow = maxPerWindow;
        this.windowMillis = windowMinutes * 60_000;
    }

    public void assertAllowed(String key) {
        String normalized = key == null || key.isBlank() ? "unknown" : key.trim();
        long now = Instant.now(clock).toEpochMilli();
        Deque<Long> queue = stamps.computeIfAbsent(normalized, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > windowMillis) {
                queue.pollFirst();
            }
            if (queue.size() >= maxPerWindow) {
                throw new CommentRateLimitedException();
            }
            queue.addLast(now);
        }
    }
}
