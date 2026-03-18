package com.fintech.transaction_service.transaction.Model;


import com.fintech.transaction_service.transaction.Enum.TransactionStatus;
import com.fintech.transaction_service.transaction.Enum.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    @Indexed(unique = true)
    private String idempotencyKey;

    @Indexed
    private String fromAccountId;

    @Indexed
    private String toAccountId;

    private BigDecimal amount;

    @Builder.Default
    private String currency = "CAD";

    private TransactionType type;
    private TransactionStatus status;
    private String description;

    private Map<String, Object> metadata;

    private Double fraudScore;
    private List<String> fraudFlags;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}