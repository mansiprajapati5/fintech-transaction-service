package com.fintech.transaction_service.transaction.Service;


import com.fintech.transaction_service.account.Enum.AccountStatus;
import com.fintech.transaction_service.account.Model.Account;
import com.fintech.transaction_service.account.Repository.AccountRepository;
import com.fintech.transaction_service.common.Kafka.KafkaProducerService;
import com.fintech.transaction_service.common.Kafka.TransactionEvent;
import com.fintech.transaction_service.common.exception.InsufficientFundsException;
import com.fintech.transaction_service.common.exception.InvalidRequestException;
import com.fintech.transaction_service.common.exception.NotFoundException;
import com.fintech.transaction_service.common.exception.UnauthorizedException;
import com.fintech.transaction_service.transaction.Decorator.*;
import com.fintech.transaction_service.transaction.Enum.TransactionStatus;
import com.fintech.transaction_service.transaction.Enum.TransactionType;
import com.fintech.transaction_service.transaction.Model.Transaction;
import com.fintech.transaction_service.transaction.Repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableRetry
public class TransactionServiceImpl implements TransactionService{

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;
    private final KafkaProducerService kafkaProducerService;

    @Override
    public TransactionResponse deposit(DepositRequest request, String userId) throws UnauthorizedException {
        // 1. check idempotency
        if (request.getIdempotencyKey() != null &&
                transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            log.info("Duplicate deposit request detected: {}", request.getIdempotencyKey());
            return modelMapper.map(
                    transactionRepository.findByIdempotencyKey(request.getIdempotencyKey()),
                    TransactionResponse.class
            );
        }

        // 2. get account and validate ownership
        Account account = getAccountById(request.getAccountId());

        if (!account.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't own this account");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidRequestException("Account is not active");
        }

        // 3. update balance
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        // 4. create transaction record
        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey() != null ?
                        request.getIdempotencyKey() : UUID.randomUUID().toString())
                .toAccountId(request.getAccountId())
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Deposit completed for account: {}", request.getAccountId());
        return modelMapper.map(saved, TransactionResponse.class);
    }
    private Account getAccountById(String accountId){
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }
    @Override
    public TransactionResponse withdraw(WithdrawRequest request, String userId) throws UnauthorizedException, InsufficientFundsException {
        // 1. check idempotency
        if (request.getIdempotencyKey() != null &&
                transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            log.info("Duplicate withdrawal request detected: {}", request.getIdempotencyKey());
            return modelMapper.map(
                    transactionRepository.findByIdempotencyKey(request.getIdempotencyKey()),
                    TransactionResponse.class
            );
        }

        // 2. get account and validate
        Account account = getAccountById(request.getAccountId());
        if (!account.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't own this account");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidRequestException("Account is not active");
        }

        // 3. check sufficient balance
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance");
        }

        // 4. update balance
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        // 5. create transaction record
        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey() != null ?
                        request.getIdempotencyKey() : UUID.randomUUID().toString())
                .fromAccountId(request.getAccountId())
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Withdrawal completed for account: {}", request.getAccountId());
        return modelMapper.map(saved, TransactionResponse.class);
    }

    @Override
    public TransactionResponse transfer(TransferRequest request, String userId) throws InsufficientFundsException, UnauthorizedException {

        // check idempotency
        if (request.getIdempotencyKey() != null &&
                transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return modelMapper.map(
                    transactionRepository.findByIdempotencyKey(request.getIdempotencyKey()),
                    TransactionResponse.class
            );
        }

        // fetch fresh accounts
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new NotFoundException("Source account not found"));

        if (!fromAccount.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't own this account");
        }

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidRequestException("Source account is not active");
        }

        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new NotFoundException("Destination account not found"));

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidRequestException("Destination account is not active");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance");
        }

        // generate idempotency key
        String idempotencyKey = request.getIdempotencyKey() != null ?
                request.getIdempotencyKey() : UUID.randomUUID().toString();

        // save transaction as PENDING first
        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .currency(fromAccount.getCurrency())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();
        Transaction saved = transactionRepository.save(transaction);

        // update balances with manual retry
        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                // fetch fresh every attempt
                Account freshFrom = accountRepository.findById(request.getFromAccountId()).get();
                Account freshTo = accountRepository.findById(request.getToAccountId()).get();

                freshFrom.setBalance(freshFrom.getBalance().subtract(request.getAmount()));
                freshTo.setBalance(freshTo.getBalance().add(request.getAmount()));

                accountRepository.save(freshFrom);
                accountRepository.save(freshTo);

                // update transaction to completed
                saved.setStatus(TransactionStatus.COMPLETED);
                transactionRepository.save(saved);

                // publish kafka event
                TransactionEvent event = TransactionEvent.builder()
                        .transactionId(saved.getId())
                        .fromAccountId(saved.getFromAccountId())
                        .toAccountId(saved.getToAccountId())
                        .amount(saved.getAmount())
                        .currency(saved.getCurrency())
                        .type(saved.getType().name())
                        .userId(userId)
                        .timestamp(LocalDateTime.now())
                        .build();
                kafkaProducerService.publishTransactionInitiated(event);

                log.info("Transfer completed: {}", saved.getId());
                return modelMapper.map(saved, TransactionResponse.class);

            } catch (OptimisticLockingFailureException e) {
                attempt++;
                log.warn("Optimistic locking conflict, attempt {}/{}", attempt, maxRetries);
                if (attempt == maxRetries) {
                    // mark transaction as failed
                    saved.setStatus(TransactionStatus.FAILED);
                    transactionRepository.save(saved);
                    throw new InvalidRequestException("Transfer failed due to concurrent modification. Please try again.");
                }
                try {
                    Thread.sleep(100); // wait 100ms before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return modelMapper.map(saved, TransactionResponse.class);
    }
    @Override
    public TransactionResponse getTransactionById(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        return modelMapper.map(transaction, TransactionResponse.class);
    }

    @Override
    public Page<TransactionResponse> getTransactionHistory(String accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository
                .findByFromAccountIdOrToAccountId(accountId, accountId, pageable)
                .map(transaction -> modelMapper.map(transaction, TransactionResponse.class));
    }

    @Override
    public TransactionResponse payment(PaymentRequest request, String userId) throws InsufficientFundsException, UnauthorizedException {
        if (request.getIdempotencyKey() != null &&
                transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            return modelMapper.map(
                    transactionRepository.findByIdempotencyKey(request.getIdempotencyKey()),
                    TransactionResponse.class
            );
        }

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't own this account");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidRequestException("Account is not active");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey() != null ?
                        request.getIdempotencyKey() : UUID.randomUUID().toString())
                .fromAccountId(request.getAccountId())
                .toAccountId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(account.getCurrency())
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return modelMapper.map(saved, TransactionResponse.class);
    }
}
