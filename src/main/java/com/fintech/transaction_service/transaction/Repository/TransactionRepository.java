package com.fintech.transaction_service.transaction.Repository;

import com.fintech.transaction_service.transaction.Model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TransactionRepository  extends MongoRepository<Transaction, String> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByFromAccountId(String fromAccountId, Pageable pageable);

    Page<Transaction> findByFromAccountIdOrToAccountId(
            String fromAccountId,
            String toAccountId,
            Pageable pageable
    );
}
