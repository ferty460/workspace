package com.example.workspace.service;

import com.example.workspace.exception.InvalidCredentialsException;
import com.example.workspace.exception.UserAlreadyExistsException;
import com.example.workspace.model.User;
import com.example.workspace.model.dto.AuthRequestDto;
import com.example.workspace.model.dto.AuthResponseDto;
import com.example.workspace.model.dto.UserResponseDto;
import com.example.workspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserResponseDto register(AuthRequestDto requestDto) {
        String login = requestDto.login();
        if (userRepository.findByLogin(login).isPresent()) {
            log.warn("Registration attempt with existing login='{}'", login);
            throw new UserAlreadyExistsException(login);
        }

        User registeredUser = new User();
        registeredUser.setLogin(login);
        registeredUser.setPassword(passwordEncoder.encode(requestDto.password()));

        User addedUser = userRepository.add(registeredUser);

        log.info("User registered: login='{}', id={}", login, addedUser.getId());
        return new UserResponseDto(addedUser.getId(), login);
    }

    public AuthResponseDto login(AuthRequestDto requestDto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestDto.login(), requestDto.password())
            );
            UserDetails principal = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateAccessToken(Objects.requireNonNull(principal));

            log.info("User logged in: login='{}'", requestDto.login());
            return new AuthResponseDto(token);
        } catch (AuthenticationException e) {
            log.warn("Failed login attempt: login='{}'", requestDto.login());
            throw new InvalidCredentialsException();
        }
    }

}
