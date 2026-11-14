package com.example.workspace.service;

import io.jsonwebtoken.ExpiredJwtException;
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

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, TTL);
    }

    private UserDetails user() {
        return new User("alice", "irrelevant", List.of(new SimpleGrantedAuthority("ROLE_USER")));
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
            String token = jwtService.generateAccessToken(user());

            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("embeds the username as the token subject")
        void embedsUsernameAsSubject() {
            String token = jwtService.generateAccessToken(user());

            assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        }
    }

    @Nested
    @DisplayName("isValid")
    class IsValid {

        @Test
        @DisplayName("returns true for a fresh token and the matching user")
        void returnsTrueForMatchingUser() {
            String token = jwtService.generateAccessToken(user());

            assertThat(jwtService.isValid(token, user())).isTrue();
        }

        @Test
        @DisplayName("returns false when the token belongs to another user")
        void returnsFalseForDifferentUser() {
            String token = jwtService.generateAccessToken(user());

            assertThat(jwtService.isValid(token, anotherUser())).isFalse();
        }

        @Test
        @DisplayName("returns false when the token signature is tampered with")
        void returnsFalseForTamperedToken() {
            String token = jwtService.generateAccessToken(user());
            String tampered = token.substring(0, token.length() - 2) + "xx";

            assertThat(jwtService.isValid(tampered, user())).isFalse();
        }

        @Test
        @DisplayName("returns false for an expired token")
        void returnsFalseForExpiredToken() {
            JwtService expired = new JwtService(SECRET, Duration.ofSeconds(-60));
            String token = expired.generateAccessToken(user());

            assertThat(expired.isValid(token, user())).isFalse();
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("throws ExpiredJwtException when the token has expired")
        void throwsOnExpiredToken() {
            JwtService expired = new JwtService(SECRET, Duration.ofSeconds(-60));
            String token = expired.generateAccessToken(user());

            assertThatThrownBy(() -> expired.extractUsername(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("reports an expiration date in the past for an expired token")
        void expiredTokenHasExpirationInThePast() {
            JwtService expired = new JwtService(SECRET, Duration.ofSeconds(-60));
            String token = expired.generateAccessToken(user());

            assertThatThrownBy(() -> expired.extractUsername(token))
                    .isInstanceOf(ExpiredJwtException.class)
                    .satisfies(ex -> {
                        ExpiredJwtException e = (ExpiredJwtException) ex;
                        assertThat(e.getClaims().getExpiration()).isBefore(new Date());
                    });
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("rejects a secret shorter than 32 bytes")
        void rejectsTooShortSecret() {
            assertThatThrownBy(() -> new JwtService("short", TTL))
                    .isInstanceOf(WeakKeyException.class);
        }
    }

}