package com.example.workspace.finance.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponseDto(

        Long id,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String description,
        Instant createdAt

) {}