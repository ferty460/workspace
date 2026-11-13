package com.example.workspace.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ApiException {

    public UserAlreadyExistsException(String login) {
        super("User with login '" + login + "' already exists", HttpStatus.CONFLICT);
    }

}
