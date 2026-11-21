package com.example.workspace.finance.exception;

import com.example.workspace.core.exception.ApiException;
import org.springframework.http.HttpStatus;

public class SameAccountTransferException extends ApiException {

    public SameAccountTransferException(Long accountId) {
        super("Source and target account must differ (accountId=" + accountId + ")", HttpStatus.BAD_REQUEST);
    }

}
