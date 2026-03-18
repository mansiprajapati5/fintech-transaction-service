package com.fintech.transaction_service.account.Repository;

import com.fintech.transaction_service.account.Model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository  extends MongoRepository<Account, String> {
    Optional<Account> findByUserId(String userId);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findAllByUserId(String userId);

    boolean existsByUserId(String userId);

}
