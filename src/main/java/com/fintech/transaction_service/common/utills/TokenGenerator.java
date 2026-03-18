package com.fintech.transaction_service.common.utills;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TokenGenerator {
    public String generateToken() {
        return UUID.randomUUID().toString();
    }
}