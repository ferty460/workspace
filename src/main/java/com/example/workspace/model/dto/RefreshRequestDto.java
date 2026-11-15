package com.example.workspace.model.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(

        @NotBlank(message = "Refresh token is required")
        String refreshToken

) {
}
