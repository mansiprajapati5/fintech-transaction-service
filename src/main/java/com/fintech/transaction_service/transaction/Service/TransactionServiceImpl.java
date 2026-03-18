package com.fintech.transaction_service.transaction.Service;

import com.fintech.transaction_service.account.Enum.AccountStatus;
import com.fintech.transaction_service.account.Model.Account;
import com.fintech.transaction_service.account.Repository.AccountRepository;
import com.fintech.transaction_service.common.exception.InsufficientFundsException;
import com.fintech.transaction_service.common.exception.InvalidRequestException;
import com.fintech.transaction_service.common.exception.NotFoundException;
import com.fintech.transaction_service.common.exception.UnauthorizedException;
import com.fintech.transaction_service.transaction.Decorator.DepositRequest;
import com.fintech.transaction_service.transaction.Decorator.TransactionResponse;
import com.fintech.transaction_service.transaction.Decorator.TransferRequest;
import com.fintech.transaction_service.transaction.Decorator.WithdrawRequest;
import com.fintech.transaction_service.transaction.Enum.TransactionStatus;
import com.fintech.transaction_service.transaction.Enum.TransactionType;
import com.fintech.transaction_service.transaction.Model.Transaction;
import com.fintech.transaction_service.transaction.Repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService{

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;

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
    public TransactionResponse transfer(TransferRequest request, String userId) throws UnauthorizedException, InsufficientFundsException {
        // 1. check idempotency
        if (request.getIdempotencyKey() != null &&
                transactionRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            log.info("Duplicate transfer request detected: {}", request.getIdempotencyKey());
            return modelMapper.map(
                    transactionRepository.findByIdempotencyKey(request.getIdempotencyKey()),
                    TransactionResponse.class
            );
        }

        // 2. get source account and validate ownership
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new NotFoundException("Source account not found"));

        if (!fromAccount.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't own this account");
        }

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidRequestException("Source account is not active");
        }

        // 3. get destination account
        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new NotFoundException("Destination account not found"));

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidRequestException("Destination account is not active");
        }

        // 4. check sufficient balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient balance");
        }

        // 5. update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // 6. create transaction record
        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey() != null ?
                        request.getIdempotencyKey() : UUID.randomUUID().toString())
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .currency(fromAccount.getCurrency())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING) // pending until fraud check
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transfer initiated from: {} to: {}", request.getFromAccountId(), request.getToAccountId());
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
}
