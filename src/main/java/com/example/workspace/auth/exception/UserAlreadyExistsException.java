package com.example.workspace.auth.exception;

import com.example.workspace.core.exception.ApiException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ApiException {

    public UserAlreadyExistsException(String login) {
        super("User with login '" + login + "' already exists", HttpStatus.CONFLICT);
    }

}
