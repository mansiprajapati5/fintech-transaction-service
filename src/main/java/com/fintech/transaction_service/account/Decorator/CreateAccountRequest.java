package com.fintech.transaction_service.account.Decorator;

import com.fintech.transaction_service.account.Enum.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {


    @NotNull(message = "Account type is required")
    public AccountType accountType;

    @Builder.Default
    public String currency = "CAD";

    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit = BigDecimal.ZERO; // optional, defaults to 0
}
