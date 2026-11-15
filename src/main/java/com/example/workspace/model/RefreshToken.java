package com.example.workspace.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "tokenHash", unique = true),
        @Index(name = "idx_refresh_token_user", columnList = "userId"),
        @Index(name = "idx_refresh_token_family", columnList = "familyId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private String familyId;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private String replacedByHash;

    private String userAgent;

    private String ip;

    @Column(nullable = false)
    private Instant createdAt;

}