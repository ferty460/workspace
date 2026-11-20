package com.example.workspace.finance.model.dto;

import java.math.BigDecimal;

public record AccountResponseDto(

        Long id,
        String name,
        BigDecimal money

) {
}
