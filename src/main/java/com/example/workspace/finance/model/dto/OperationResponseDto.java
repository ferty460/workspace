package com.example.workspace.finance.model.dto;

import com.example.workspace.finance.model.OperationType;

import java.math.BigDecimal;

public record OperationResponseDto(

        Long id,
        Long accountId,
        OperationType type,
        BigDecimal amount,
        String description

) {
}
