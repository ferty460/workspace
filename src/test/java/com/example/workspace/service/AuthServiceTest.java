package com.example.workspace.service;

import com.example.workspace.exception.InvalidCredentialsException;
import com.example.workspace.exception.UserAlreadyExistsException;
import com.example.workspace.model.User;
import com.example.workspace.model.dto.AuthRequestDto;
import com.example.workspace.model.dto.AuthResponseDto;
import com.example.workspace.model.dto.UserResponseDto;
import com.example.workspace.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtService jwtService;

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
        @DisplayName("throws UserAlreadyExistsException when the login is taken")
        void throwsWhenLoginTaken() {
            when(userRepository.findByLogin("alice"))
                    .thenReturn(Optional.of(new User(1L, "alice", "hash")));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userRepository, never()).add(any());
        }

        @Test
        @DisplayName("does not touch the repository when the login is taken")
        void doesNotSaveWhenLoginTaken() {
            when(userRepository.findByLogin("alice"))
                    .thenReturn(Optional.of(new User(1L, "alice", "hash")));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verifyNoInteractions(passwordEncoder);
            verify(userRepository, never()).add(any());
        }

        @Test
        @DisplayName("encodes the password and saves the user")
        void encodesPasswordAndSaves() {
            when(userRepository.findByLogin("alice")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("encoded");
            when(userRepository.add(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(42L);
                return u;
            });

            UserResponseDto response = authService.register(request);

            assertThat(response.userId()).isEqualTo(42L);
            assertThat(response.login()).isEqualTo("alice");
        }

        @Test
        @DisplayName("passes the encoded password, not the raw one, to the repository")
        void passesEncodedPasswordToRepository() {
            when(userRepository.findByLogin("alice")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("encoded");
            when(userRepository.add(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(42L);
                return u;
            });

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).add(captor.capture());

            User saved = captor.getValue();
            assertThat(saved.getLogin()).isEqualTo("alice");
            assertThat(saved.getPassword()).isEqualTo("encoded");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns the access token on valid credentials")
        void returnsTokenOnSuccess() {
            UserDetails principal = principal();
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(principal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtService.generateAccessToken(principal)).thenReturn("jwt-token");

            AuthResponseDto result = authService.login(request);

            assertThat(result.accessToken()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("does not store the authentication in the security context")
        void doesNotPopulateSecurityContext() {
            UserDetails principal = principal();
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(principal);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtService.generateAccessToken(principal)).thenReturn("jwt-token");

            authService.login(request);

            assertThat(org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("throws InvalidCredentialsException on bad password")
        void throwsInvalidCredentialsOnBadPassword() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("bad"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("does not generate a token when authentication fails")
        void doesNotGenerateTokenOnFailure() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("bad"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);

            verifyNoInteractions(jwtService);
        }
    }

}