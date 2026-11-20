package com.example.workspace.finance.exception;

import com.example.workspace.core.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AccountAlreadyExistsException extends ApiException {

    public AccountAlreadyExistsException(String name) {
        super("Account with name '" + name + "' already exists", HttpStatus.CONFLICT);
    }

}
