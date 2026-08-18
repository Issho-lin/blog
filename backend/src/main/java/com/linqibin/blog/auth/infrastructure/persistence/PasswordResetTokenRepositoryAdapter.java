package com.linqibin.blog.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.linqibin.blog.auth.domain.PasswordResetToken;
import com.linqibin.blog.auth.domain.PasswordResetTokenRepository;

public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final SpringDataPasswordResetTokenRepository springDataPasswordResetTokenRepository;
    private final PasswordResetTokenEntityMapper mapper;

    public PasswordResetTokenRepositoryAdapter(
            SpringDataPasswordResetTokenRepository springDataPasswordResetTokenRepository,
            PasswordResetTokenEntityMapper mapper
    ) {
        this.springDataPasswordResetTokenRepository = springDataPasswordResetTokenRepository;
        this.mapper = mapper;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return mapper.toDomain(springDataPasswordResetTokenRepository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return springDataPasswordResetTokenRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<PasswordResetToken> findByUserId(UUID userId) {
        return springDataPasswordResetTokenRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
