package com.example.workspace.finance.exception;

import com.example.workspace.core.exception.ApiException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException(Long accountId, BigDecimal balance, BigDecimal requested) {
        super(String.format("Insufficient funds on account %d. Balance: %s, Requested: %s",
                        accountId, balance, requested), HttpStatus.BAD_REQUEST);
    }

}