package com.example.workspace.controller;

import com.example.workspace.model.dto.AuthRequestDto;
import com.example.workspace.model.dto.AuthResponseDto;
import com.example.workspace.model.dto.UserResponseDto;
import com.example.workspace.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto requestDto) {
        return ResponseEntity.ok(authService.login(requestDto));
    }

}
