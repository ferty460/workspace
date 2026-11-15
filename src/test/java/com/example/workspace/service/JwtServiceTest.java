package com.example.workspace.service;

import com.example.workspace.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long!!";
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String ISSUER = "workspace";
    private static final String AUDIENCE = "workspace-api";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(properties(SECRET, TTL, ISSUER, AUDIENCE));
    }

    private static JwtProperties properties(String secret, Duration ttl,
                                            String issuer, String audience) {
        return new JwtProperties(secret, ttl, issuer, audience);
    }

    private UserDetails user() {
        return new User("alice", "irrelevant",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private UserDetails anotherUser() {
        return new User("bob", "irrelevant", List.of());
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("returns a non-blank token")
        void returnsNonBlankToken() {
            assertThat(jwtService.generateAccessToken(user())).isNotBlank();
        }

        @Test
        @DisplayName("embeds the username as the token subject")
        void embedsUsernameAsSubject() {
            String token = jwtService.generateAccessToken(user());

            assertThat(jwtService.parse(token).getSubject()).isEqualTo("alice");
        }

        @Test
        @DisplayName("embeds a unique jti on every call")
        void embedsUniqueJti() {
            String first = jwtService.generateAccessToken(user());
            String second = jwtService.generateAccessToken(user());

            assertThat(jwtService.parse(first).getId())
                    .isNotBlank()
                    .isNotEqualTo(jwtService.parse(second).getId());
        }

        @Test
        @DisplayName("embeds issuer and audience from configuration")
        void embedsIssuerAndAudience() {
            Claims claims = jwtService.parse(jwtService.generateAccessToken(user()));

            assertThat(claims.getIssuer()).isEqualTo(ISSUER);
            assertThat(claims.getAudience()).containsExactly(AUDIENCE);
        }

        @Test
        @DisplayName("embeds roles from authorities")
        void embedsRoles() {
            Claims claims = jwtService.parse(jwtService.generateAccessToken(user()));

            assertThat(claims.get("roles")).isEqualTo(List.of("ROLE_USER"));
        }
    }

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("throws ExpiredJwtException for an expired token")
        void throwsOnExpiredToken() {
            JwtService expired = new JwtService(properties(SECRET, Duration.ofSeconds(-60),
                    ISSUER, AUDIENCE));
            String token = expired.generateAccessToken(user());

            assertThatThrownBy(() -> expired.parse(token))
                    .isInstanceOf(ExpiredJwtException.class)
                    .satisfies(ex -> {
                        ExpiredJwtException e = (ExpiredJwtException) ex;
                        assertThat(e.getClaims().getExpiration()).isBefore(new Date());
                    });
        }

        @Test
        @DisplayName("throws when issuer does not match")
        void throwsOnWrongIssuer() {
            JwtService foreign = new JwtService(properties(SECRET, TTL,
                    "someone-else", AUDIENCE));
            String token = foreign.generateAccessToken(user());

            assertThatThrownBy(() -> jwtService.parse(token))
                    .isInstanceOf(IncorrectClaimException.class);
        }

        @Test
        @DisplayName("throws when audience does not match")
        void throwsOnWrongAudience() {
            JwtService foreign = new JwtService(properties(SECRET, TTL,
                    ISSUER, "another-api"));
            String token = foreign.generateAccessToken(user());

            assertThatThrownBy(() -> jwtService.parse(token))
                    .isInstanceOf(IncorrectClaimException.class);
        }

        @Test
        @DisplayName("throws when signature is tampered with")
        void throwsOnTamperedSignature() {
            String token = jwtService.generateAccessToken(user());
            String tampered = token.substring(0, token.length() - 2) + "xx";

            assertThatThrownBy(() -> jwtService.parse(tampered))
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);
        }
    }

    @Nested
    @DisplayName("isValid")
    class IsValid {

        @Test
        @DisplayName("returns true for a fresh token and the matching user")
        void returnsTrueForMatchingUser() {
            String token = jwtService.generateAccessToken(user());

            assertThat(jwtService.isValid(jwtService.parse(token), user())).isTrue();
        }

        @Test
        @DisplayName("returns false when the token belongs to another user")
        void returnsFalseForDifferentUser() {
            String token = jwtService.generateAccessToken(user());

            assertThat(jwtService.isValid(jwtService.parse(token), anotherUser())).isFalse();
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("rejects a secret shorter than 32 bytes")
        void rejectsTooShortSecret() {
            assertThatThrownBy(() -> new JwtService(properties("short", TTL,
                    ISSUER, AUDIENCE)))
                    .isInstanceOf(WeakKeyException.class);
        }
    }
}