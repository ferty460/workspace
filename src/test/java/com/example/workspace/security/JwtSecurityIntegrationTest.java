package com.example.workspace.security;

import com.example.workspace.model.User;
import com.example.workspace.repository.UserRepository;
import com.example.workspace.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("JWT security integration")
class JwtSecurityIntegrationTest {

    private static final String PROTECTED_ENDPOINT = "/api/users";
    private static final String PUBLIC_ENDPOINT = "/api/home";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findByLogin("alice").isEmpty()) {
            User u = new User();
            u.setLogin("alice");
            u.setPassword(passwordEncoder.encode("secret123"));
            userRepository.add(u);
        }
    }

    private UserDetails alice() {
        return org.springframework.security.core.userdetails.User
                .withUsername("alice")
                .password("x")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Nested
    @DisplayName("protected endpoint")
    class ProtectedEndpoint {

        @Test
        @DisplayName("returns 401 without an Authorization header")
        void returns401WithoutToken() throws Exception {
            mockMvc.perform(get(PROTECTED_ENDPOINT))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 200 with a valid Bearer token")
        void returns200WithValidToken() throws Exception {
            String token = jwtService.generateAccessToken(alice());

            mockMvc.perform(get(PROTECTED_ENDPOINT).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 401 with a malformed token")
        void returns401WithMalformedToken() throws Exception {
            mockMvc.perform(get(PROTECTED_ENDPOINT).header("Authorization", "Bearer garbage.token.here"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 401 with a token signed by a different key")
        void returns401WithTokenSignedByDifferentKey() throws Exception {
            JwtService foreignJwtService = new JwtService(
                    "another-secret-key-at-least-32-bytes-long!!",
                    Duration.ofMinutes(15)
            );
            String foreignToken = foreignJwtService.generateAccessToken(alice());

            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + foreignToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 401 with an expired token")
        void returns401WithExpiredToken() throws Exception {
            JwtService expiredJwtService = new JwtService(
                    "test-secret-key-at-least-32-bytes-long!!",
                    Duration.ofSeconds(-60)
            );
            String expiredToken = expiredJwtService.generateAccessToken(alice());

            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + expiredToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 401 when the Authorization header is not a Bearer scheme")
        void returns401WithNonBearerScheme() throws Exception {
            String token = jwtService.generateAccessToken(alice());

            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Basic " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("public endpoint")
    class PublicEndpoint {

        @Test
        @DisplayName("is accessible without a token")
        void isAccessibleWithoutToken() throws Exception {
            mockMvc.perform(get(PUBLIC_ENDPOINT))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("is accessible with a valid token")
        void isAccessibleWithValidToken() throws Exception {
            String token = jwtService.generateAccessToken(alice());

            mockMvc.perform(get(PUBLIC_ENDPOINT)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

}