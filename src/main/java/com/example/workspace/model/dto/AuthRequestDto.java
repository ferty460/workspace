package com.example.workspace.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequestDto(

        @NotBlank(message = "Login is required")
        @Size(min = 3, max = 64, message = "Login must contain from 3 to 64 characters.")
        String login,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 32, message = "Password must contain from 6 to 32 characters.")
        String password

) {
}
