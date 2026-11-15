package com.example.workspace.controller;

import com.example.workspace.exception.InvalidCredentialsException;
import com.example.workspace.model.dto.AuthRequestDto;
import com.example.workspace.model.dto.AuthResponseDto;
import com.example.workspace.model.dto.RefreshRequestDto;
import com.example.workspace.model.dto.UserResponseDto;
import com.example.workspace.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequestDto requestDto) {
        UserResponseDto registeredUser = authService.register(requestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody AuthRequestDto requestDto,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.login(
                requestDto,
                httpRequest.getHeader(HttpHeaders.USER_AGENT),
                httpRequest.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(
            @Valid @RequestBody RefreshRequestDto request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.refresh(
                request.refreshToken(),
                httpRequest.getHeader(HttpHeaders.USER_AGENT),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestBody(required = false) RefreshRequestDto request
    ) {
        String accessToken = extractBearer(authHeader);
        String refreshToken = request != null ? request.refreshToken() : null;
        authService.logout(accessToken, refreshToken);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
            Authentication authentication,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        authService.logoutAll(authentication.getName(), extractBearer(authHeader));
        return ResponseEntity.noContent().build();
    }

    private String extractBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new InvalidCredentialsException();
        }

        return header.substring(7);
    }

}
