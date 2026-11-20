package com.example.workspace.auth.service;

import com.example.workspace.auth.exception.InvalidCredentialsException;
import com.example.workspace.auth.exception.UserAlreadyExistsException;
import com.example.workspace.auth.model.RevokedAccessToken;
import com.example.workspace.auth.model.Role;
import com.example.workspace.auth.model.User;
import com.example.workspace.auth.model.UserPrincipal;
import com.example.workspace.auth.model.dto.AuthRequestDto;
import com.example.workspace.auth.model.dto.AuthResponseDto;
import com.example.workspace.auth.model.dto.UserResponseDto;
import com.example.workspace.auth.repository.RevokedAccessTokenRepository;
import com.example.workspace.auth.repository.UserRepository;
import com.example.workspace.core.exception.NotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    @Transactional
    public UserResponseDto register(AuthRequestDto requestDto) {
        String login = requestDto.login();

        User user = new User();
        user.setLogin(login);
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(requestDto.password()));

        try {
            User saved = userRepository.save(user);
            return new UserResponseDto(saved.getId(), saved.getLogin());
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException(login);
        }
    }

    public AuthResponseDto login(AuthRequestDto requestDto, String userAgent, String ip) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestDto.login(), requestDto.password())
            );

            UserDetails principal = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByLogin(Objects.requireNonNull(principal).getUsername())
                    .orElseThrow(() -> new NotFoundException("User not found"));

            String accessToken = jwtService.generateAccessToken(principal);
            String refreshToken = refreshTokenService.createRefreshToken(user.getId(), null, userAgent, ip);

            log.info("User logged in: login='{}'", requestDto.login());
            return new AuthResponseDto(accessToken, refreshToken);
        } catch (AuthenticationException e) {
            log.warn("Failed login attempt: login='{}'", requestDto.login());
            throw new InvalidCredentialsException();
        }
    }

    public AuthResponseDto refresh(String rawRefreshToken, String userAgent, String ip) {
        var result = refreshTokenService.rotate(rawRefreshToken, userAgent, ip);

        User user = userRepository.findById(result.userId())
                .orElseThrow(InvalidCredentialsException::new);
        UserDetails userDetails = new UserPrincipal(user);
        String newAccess = jwtService.generateAccessToken(userDetails);

        log.info("Token refreshed for userId={}", result.userId());
        return new AuthResponseDto(newAccess, result.newRefreshToken());
    }

    public void logout(String rawAccessToken, String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }

        revokeAccessToken(rawAccessToken);

        log.info("User logged out");
    }

    public void logoutAll(String username, String rawAccessToken) {
        User user = userRepository.findByLogin(username)
                .orElseThrow(InvalidCredentialsException::new);

        refreshTokenService.revokeAllForUser(user.getId());
        revokeAccessToken(rawAccessToken);
    }

    private void revokeAccessToken(String rawAccessToken) {
        try {
            Claims claims = jwtService.parse(rawAccessToken);
            revokedAccessTokenRepository.save(new RevokedAccessToken(
                    claims.getId(), claims.getIssuer(),
                    claims.getExpiration().toInstant()));
        } catch (JwtException e) {
            log.debug("Access token already invalid, skip denylist: {}", e.getMessage());
        }
    }

}
