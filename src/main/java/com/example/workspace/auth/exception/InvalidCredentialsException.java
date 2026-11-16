package com.example.workspace.auth.exception;

import com.example.workspace.core.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super("Wrong login or password", HttpStatus.UNAUTHORIZED);
    }

}
