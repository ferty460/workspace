package com.example.workspace.finance.model.dto;

import java.math.BigDecimal;

public record AccountDetailsDto(

        Long id,
        String name,
        BigDecimal money,
        long operationsCount

) {
}
