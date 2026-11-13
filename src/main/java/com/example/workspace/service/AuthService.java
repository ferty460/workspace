package com.example.workspace.service;

import com.example.workspace.model.User;
import com.example.workspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public User register(User user) {
        String login = user.getLogin();
        if (userRepository.findByLogin(login).isPresent()) {
            throw new IllegalStateException("User with login '" + login + "' already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.add(user);
    }

    public void login(User user) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getLogin(), user.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Wrong username or password");
        }
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

}
