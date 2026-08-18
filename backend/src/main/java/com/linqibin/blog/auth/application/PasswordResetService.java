package com.linqibin.blog.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.linqibin.blog.auth.domain.PasswordResetToken;
import com.linqibin.blog.auth.domain.PasswordResetTokenRepository;
import com.linqibin.blog.auth.domain.User;
import com.linqibin.blog.auth.domain.UserRepository;
import com.linqibin.blog.auth.exception.InvalidResetTokenException;

public class PasswordResetService {

    static final String REQUEST_ACCEPTED_MESSAGE = "如果该邮箱存在，我们已发送重置说明";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetNotifier notifier;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Duration tokenTtl;
    private final String publicSiteUrl;
    private final Supplier<UUID> idSupplier;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetNotifier notifier,
            PasswordEncoder passwordEncoder,
            Clock clock,
            Duration tokenTtl,
            String publicSiteUrl
    ) {
        this(userRepository, tokenRepository, notifier, passwordEncoder, clock, tokenTtl, publicSiteUrl, UUID::randomUUID);
    }

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetNotifier notifier,
            PasswordEncoder passwordEncoder,
            Clock clock,
            Duration tokenTtl,
            String publicSiteUrl,
            Supplier<UUID> idSupplier
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.tokenRepository = Objects.requireNonNull(tokenRepository);
        this.notifier = Objects.requireNonNull(notifier);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.clock = Objects.requireNonNull(clock);
        this.tokenTtl = Objects.requireNonNull(tokenTtl);
        this.publicSiteUrl = publicSiteUrl == null || publicSiteUrl.isBlank()
                ? "http://localhost:3000"
                : publicSiteUrl.replaceAll("/$", "");
        this.idSupplier = Objects.requireNonNull(idSupplier);
    }

    public String requestReset(String email) {
        User user = userRepository.findByEmail(email == null ? "" : email.trim()).orElse(null);
        if (user == null) {
            return REQUEST_ACCEPTED_MESSAGE;
        }
        Instant now = Instant.now(clock);
        String rawToken = randomToken();
        PasswordResetToken token = new PasswordResetToken(
                idSupplier.get(),
                user.id(),
                sha256(rawToken),
                now.plus(tokenTtl),
                null
        );
        tokenRepository.save(token);
        notifier.sendResetLink(user.email(), publicSiteUrl + "/admin/reset-password?token=" + rawToken);
        return REQUEST_ACCEPTED_MESSAGE;
    }

    public void resetPassword(String rawToken, String newPassword) {
        AuthPasswords.assertStrong(newPassword);
        Instant now = Instant.now(clock);
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256(rawToken == null ? "" : rawToken.trim()))
                .filter(item -> item.isUsable(now))
                .orElseThrow(InvalidResetTokenException::new);
        User user = userRepository.findById(token.userId()).orElseThrow(InvalidResetTokenException::new);
        userRepository.save(user.withPasswordHash(passwordEncoder.encode(newPassword), now));
        tokenRepository.save(token.markUsed(now));
        for (PasswordResetToken other : tokenRepository.findByUserId(user.id())) {
            if (!other.id().equals(token.id()) && other.usedAt() == null) {
                tokenRepository.save(other.markUsed(now));
            }
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
