package com.example.workspace.service;

import com.example.workspace.model.User;
import com.example.workspace.model.UserPrincipal;
import com.example.workspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(username).orElseThrow(
                () -> new UsernameNotFoundException("User with login '" + username + "' not found")
        );

        return new UserPrincipal(user);
    }

}
