package com.fintech.transaction_service.common.exception;

public class MissingParameter extends RuntimeException {

    public MissingParameter() {
    }

    public MissingParameter(String message) {
        super(message);
    }
}
