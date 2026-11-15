package com.example.workspace.repository;

import com.example.workspace.model.RevokedAccessToken;
import com.example.workspace.model.RevokedAccessTokenId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, RevokedAccessTokenId> {

    boolean existsByJtiAndIssuer(String jti, String issuer);
    void deleteAllByExpiresAtBefore(Instant cutoff);

}
