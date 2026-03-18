package com.fintech.transaction_service.account.Decorator;

import com.fintech.transaction_service.account.Enum.AccountStatus;
import com.fintech.transaction_service.account.Enum.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {
    public String id;
    public String accountNumber;
    public String userId;
    public BigDecimal balance;
    public String currency;
    public AccountType accountType;
    public AccountStatus status;
    public LocalDateTime createdAt;
}
