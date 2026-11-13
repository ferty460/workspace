package com.example.workspace.service;

import com.example.workspace.model.User;
import com.example.workspace.model.dto.AuthRequestDto;
import com.example.workspace.model.dto.UserResponseDto;
import com.example.workspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UserResponseDto register(AuthRequestDto requestDto) {
        String login = requestDto.login();
        if (userRepository.findByLogin(login).isPresent()) {
            throw new IllegalStateException("User with login '" + login + "' already exists");
        }

        User registeredUser = new User();
        registeredUser.setLogin(login);
        registeredUser.setPassword(passwordEncoder.encode(requestDto.password()));

        User addedUser = userRepository.add(registeredUser);

        return new UserResponseDto(addedUser.getId(), login);
    }

    public void login(AuthRequestDto requestDto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestDto.login(), requestDto.password())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AuthenticationException e) {
            throw new RuntimeException("Wrong login or password");
        }
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

}
