package com.example.workspace.service;

import com.example.workspace.auth.exception.InvalidCredentialsException;
import com.example.workspace.auth.exception.UserAlreadyExistsException;
import com.example.workspace.auth.model.RevokedAccessToken;
import com.example.workspace.auth.service.AuthService;
import com.example.workspace.auth.service.JwtService;
import com.example.workspace.auth.service.RefreshTokenService;
import com.example.workspace.auth.model.Role;
import com.example.workspace.auth.model.User;
import com.example.workspace.auth.model.dto.AuthRequestDto;
import com.example.workspace.auth.model.dto.AuthResponseDto;
import com.example.workspace.auth.model.dto.UserResponseDto;
import com.example.workspace.auth.repository.RevokedAccessTokenRepository;
import com.example.workspace.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock
    JwtService jwtService;
    @Mock
    RefreshTokenService refreshTokenService;
    @Mock RevokedAccessTokenRepository revokedAccessTokenRepository;

    @InjectMocks
    AuthService authService;

    private final AuthRequestDto request = new AuthRequestDto("alice", "secret123");

    private UserDetails principal() {
        return org.springframework.security.core.userdetails.User
                .withUsername("alice")
                .password("x")
                .authorities("ROLE_USER")
                .build();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("throws UserAlreadyExistsException on unique constraint violation")
        void throwsWhenLoginTaken() {
            when(passwordEncoder.encode("secret123")).thenReturn("encoded");
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("dup"));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class);
        }

        @Test
        @DisplayName("encodes the password and saves the user with ROLE_USER")
        void encodesPasswordAndSaves() {
            when(passwordEncoder.encode("secret123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(42L);
                return u;
            });

            UserResponseDto response = authService.register(request);

            assertThat(response.userId()).isEqualTo(42L);
            assertThat(response.login()).isEqualTo("alice");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getPassword()).isEqualTo("encoded");
            assertThat(saved.getRole()).isEqualTo(Role.USER);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns access and refresh tokens on success")
        void returnsTokensOnSuccess() {
            UserDetails principal = principal();
            Authentication authentication = mock(Authentication.class);
            User user = new User(1L, "alice", "hash", Role.USER);

            when(authentication.getPrincipal()).thenReturn(principal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findByLogin("alice")).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(principal)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(eq(1L), isNull(), anyString(), anyString()))
                    .thenReturn("refresh-token");

            AuthResponseDto result = authService.login(request, "UA", "127.0.0.1");

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("passes userAgent and ip to refresh token service")
        void passesMetadataToRefreshService() {
            UserDetails principal = principal();
            Authentication authentication = mock(Authentication.class);
            User user = new User(1L, "alice", "hash", Role.USER);

            when(authentication.getPrincipal()).thenReturn(principal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findByLogin("alice")).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(principal)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(anyLong(), isNull(), anyString(), anyString()))
                    .thenReturn("refresh-token");

            authService.login(request, "Mozilla", "10.0.0.1");

            verify(refreshTokenService).createRefreshToken(1L, null, "Mozilla", "10.0.0.1");
        }

        @Test
        @DisplayName("throws InvalidCredentialsException on bad password")
        void throwsInvalidCredentialsOnBadPassword() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("bad"));

            assertThatThrownBy(() -> authService.login(request, "UA", "ip"))
                    .isInstanceOf(InvalidCredentialsException.class);

            verifyNoInteractions(jwtService, refreshTokenService);
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("returns new access token and forwards new refresh token")
        void returnsNewTokens() {
            User user = new User(1L, "alice", "hash", Role.USER);

            when(refreshTokenService.rotate("old-refresh", "UA", "ip"))
                    .thenReturn(new RefreshTokenService.RotationResult(1L, "new-refresh"));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(any(UserDetails.class)))
                    .thenReturn("new-access");

            AuthResponseDto result = authService.refresh("old-refresh", "UA", "ip");

            assertThat(result.accessToken()).isEqualTo("new-access");
            assertThat(result.refreshToken()).isEqualTo("new-refresh");
        }

        @Test
        @DisplayName("propagates InvalidCredentialsException when user is missing")
        void throwsWhenUserMissing() {
            when(refreshTokenService.rotate(anyString(), anyString(), anyString()))
                    .thenReturn(new RefreshTokenService.RotationResult(99L, "new-refresh"));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("old", "UA", "ip"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        private Claims claimsOf(String jti) {
            return Jwts.claims()
                    .id(jti)
                    .issuer("workspace")
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .build();
        }

        @Test
        @DisplayName("revokes refresh token when provided")
        void revokesRefreshToken() {
            when(jwtService.parse("access")).thenReturn(claimsOf("jti-1"));

            authService.logout("access", "refresh");

            verify(refreshTokenService).revoke("refresh");
        }

        @Test
        @DisplayName("skips refresh revocation when null or blank")
        void skipsRefreshWhenNull() {
            when(jwtService.parse("access")).thenReturn(claimsOf("jti-1"));

            authService.logout("access", null);
            authService.logout("access", "  ");

            verify(refreshTokenService, never()).revoke(anyString());
        }

        @Test
        @DisplayName("adds access jti to denylist")
        void addsToDenylist() {
            when(jwtService.parse("access")).thenReturn(claimsOf("jti-42"));

            authService.logout("access", null);

            ArgumentCaptor<RevokedAccessToken> captor =
                    ArgumentCaptor.forClass(RevokedAccessToken.class);
            verify(revokedAccessTokenRepository).save(captor.capture());
            assertThat(captor.getValue().getJti()).isEqualTo("jti-42");
            assertThat(captor.getValue().getIssuer()).isEqualTo("workspace");
        }

        @Test
        @DisplayName("ignores expired access token")
        void ignoresExpiredAccessToken() {
            when(jwtService.parse("access"))
                    .thenThrow(new ExpiredJwtException(null, null, "expired"));

            authService.logout("access", "refresh");

            verify(revokedAccessTokenRepository, never()).save(any());
            verify(refreshTokenService).revoke("refresh");
        }
    }

    @Nested
    @DisplayName("logoutAll")
    class LogoutAll {

        @Test
        @DisplayName("revokes all refresh tokens and denylists access")
        void revokesEverything() {
            User user = new User(1L, "alice", "hash", Role.USER);
            when(userRepository.findByLogin("alice")).thenReturn(Optional.of(user));
            when(jwtService.parse("access")).thenReturn(Jwts.claims()
                    .id("jti-1").issuer("workspace")
                    .expiration(new Date(System.currentTimeMillis() + 60_000))
                    .build());

            authService.logoutAll("alice", "access");

            verify(refreshTokenService).revokeAllForUser(1L);
            verify(revokedAccessTokenRepository).save(any());
        }

        @Test
        @DisplayName("throws when user is missing")
        void throwsWhenUserMissing() {
            when(userRepository.findByLogin("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.logoutAll("ghost", "access"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

}