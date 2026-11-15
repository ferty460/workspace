package com.example.workspace.repository;

import com.example.workspace.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Modifying
    @Query("""
    UPDATE RefreshToken t
    SET t.revokedAt = :now
    WHERE t.tokenHash = :tokenHash
      AND t.revokedAt IS NULL
      AND t.expiresAt > :now
    """)
    int revokeActiveByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
    UPDATE RefreshToken t
    SET t.replacedByHash = :newHash
    WHERE t.tokenHash = :oldHash
    """)
    int updateReplacedByHash(
            @Param("oldHash") String oldHash,
            @Param("newHash") String newHash
    );

    @Modifying
    @Query("""
    UPDATE RefreshToken t
    SET t.revokedAt = :now
    WHERE t.familyId = :familyId
      AND t.revokedAt IS NULL
    """)
    int revokeFamily(@Param("familyId") String familyId,
                     @Param("now") Instant now);

    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);
    void deleteAllByExpiresAtBefore(Instant cutoff);

}
