package com.example.workspace.finance.model.dto;

import com.example.workspace.finance.model.OperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OperationRequestDto(

        @NotNull
        OperationType type,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @Size(max = 4096)
        String description,

        Long accountId

) {}
