package com.example.workspace.service;

import com.example.workspace.exception.InvalidCredentialsException;
import com.example.workspace.exception.UserAlreadyExistsException;
import com.example.workspace.model.User;
import com.example.workspace.model.dto.AuthRequestDto;
import com.example.workspace.model.dto.UserResponseDto;
import com.example.workspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

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

    public void login(AuthRequestDto requestDto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestDto.login(), requestDto.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("User logged in: login='{}'", requestDto.login());
        } catch (AuthenticationException e) {
            log.warn("Failed login attempt: login='{}'", requestDto.login());
            throw new InvalidCredentialsException();
        }
    }

    public void logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = auth != null ? auth.getName() : "anonymous";
        log.info("User logged out: login='{}'", login);

        SecurityContextHolder.clearContext();
    }

}
