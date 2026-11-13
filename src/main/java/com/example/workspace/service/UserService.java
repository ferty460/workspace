package com.example.workspace.service;

import com.example.workspace.model.dto.UserResponseDto;
import com.example.workspace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponseDto> getAll() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponseDto(user.getId(), user.getLogin()))
                .toList();
    }

}
