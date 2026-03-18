package com.fintech.transaction_service.common.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException() {
    }
    public NotFoundException(String message) {
        super(message);
    }
}
