package com.fintech.transaction_service.transaction.Decorator;



import com.fintech.transaction_service.transaction.Enum.TransactionStatus;
import com.fintech.transaction_service.transaction.Enum.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private String id;
    private String idempotencyKey;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private Double fraudScore;
    private List<String> fraudFlags;
    private LocalDateTime createdAt;
}
