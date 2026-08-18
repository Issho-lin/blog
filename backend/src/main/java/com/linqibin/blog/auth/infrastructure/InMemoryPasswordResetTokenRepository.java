package com.linqibin.blog.auth.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.linqibin.blog.auth.domain.PasswordResetToken;
import com.linqibin.blog.auth.domain.PasswordResetTokenRepository;

public class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final ConcurrentHashMap<UUID, PasswordResetToken> tokens = new ConcurrentHashMap<>();

    public void clear() {
        tokens.clear();
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        tokens.put(token.id(), token);
        return token;
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return tokens.values().stream()
                .filter(token -> token.tokenHash().equals(tokenHash))
                .findFirst();
    }

    @Override
    public List<PasswordResetToken> findByUserId(UUID userId) {
        return tokens.values().stream()
                .filter(token -> token.userId().equals(userId))
                .toList();
    }
}
