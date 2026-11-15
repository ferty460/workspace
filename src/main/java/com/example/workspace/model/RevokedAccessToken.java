package com.example.workspace.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "revoked_access_tokens")
@IdClass(RevokedAccessTokenId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RevokedAccessToken {

    @Id
    private String jti;

    @Id
    private String issuer;

    @Column(nullable = false)
    private Instant expiresAt;

}