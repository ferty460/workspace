package com.example.workspace.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super("Wrong login or password", HttpStatus.UNAUTHORIZED);
    }

}
