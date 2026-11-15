package com.example.workspace.service;

import com.example.workspace.exception.InvalidCredentialsException;
import com.example.workspace.model.RefreshToken;
import com.example.workspace.repository.RefreshTokenRepository;
import com.example.workspace.repository.RevokedAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    public String createRefreshToken(Long userId, String familyId, String userAgent, String ip) {
        String rawToken = generateOpaqueToken();

        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(sha256(rawToken));
        entity.setFamilyId(familyId != null ? familyId : UUID.randomUUID().toString());
        entity.setExpiresAt(Instant.now().plus(REFRESH_TTL));
        entity.setUserAgent(userAgent);
        entity.setIp(ip);
        entity.setCreatedAt(Instant.now());

        refreshTokenRepository.save(entity);

        return rawToken;
    }

    @Transactional
    public RotationResult rotate(String rawToken, String userAgent, String ip) {
        String tokenHash = sha256(rawToken);
        Instant now = Instant.now();

        RefreshToken existing = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(InvalidCredentialsException::new);

        int revoked = refreshTokenRepository.revokeActiveByTokenHash(tokenHash, now);

        if (revoked == 0) {
            refreshTokenRepository.revokeFamily(existing.getFamilyId(), Instant.now());
            throw new InvalidCredentialsException();
        }

        String newRawToken = createRefreshToken(
                existing.getUserId(),
                existing.getFamilyId(),
                userAgent,
                ip
        );

        refreshTokenRepository.updateReplacedByHash(
                tokenHash,
                sha256(newRawToken)
        );

        return new RotationResult(existing.getUserId(), newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> t.setRevokedAt(Instant.now()));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(t -> t.setRevokedAt(Instant.now()));
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpired() {
        Instant now = Instant.now();
        refreshTokenRepository.deleteAllByExpiresAtBefore(now);
        revokedAccessTokenRepository.deleteAllByExpiresAtBefore(now);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record RotationResult(Long userId, String newRefreshToken) {}

}
