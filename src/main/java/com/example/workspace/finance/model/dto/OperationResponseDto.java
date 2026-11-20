package com.example.workspace.finance.model.dto;

import java.math.BigDecimal;

public record OperationResponseDto(

        Long id,
        Long accountId,
        String type,
        BigDecimal amount,
        String description

) {
}
