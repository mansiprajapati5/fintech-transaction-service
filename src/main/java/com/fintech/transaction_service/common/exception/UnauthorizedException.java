package com.fintech.transaction_service.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedException extends Exception{
    public UnauthorizedException(String message) {
        super(message);
    }
}
