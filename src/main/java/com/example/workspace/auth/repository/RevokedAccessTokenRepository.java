package com.example.workspace.auth.repository;

import com.example.workspace.auth.model.RevokedAccessToken;
import com.example.workspace.auth.model.RevokedAccessTokenId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, RevokedAccessTokenId> {

    boolean existsByJtiAndIssuer(String jti, String issuer);
    void deleteAllByExpiresAtBefore(Instant cutoff);

}
