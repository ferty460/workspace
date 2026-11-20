package com.example.workspace.finance.model.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AccountRequestDto(

        @NotBlank
        @Size(min = 1, max = 64)
        String name,

        @NotNull
        @Digits(integer = 17, fraction = 2)
        BigDecimal balance

) {
}
