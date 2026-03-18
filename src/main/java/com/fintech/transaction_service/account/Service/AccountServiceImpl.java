package com.fintech.transaction_service.account.Service;

import com.fintech.transaction_service.account.Decorator.AccountResponse;
import com.fintech.transaction_service.account.Decorator.CreateAccountRequest;
import com.fintech.transaction_service.account.Enum.AccountStatus;
import com.fintech.transaction_service.account.Model.Account;
import com.fintech.transaction_service.account.Repository.AccountRepository;
import com.fintech.transaction_service.common.exception.NotFoundException;
import com.fintech.transaction_service.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService{


    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;

    @Override
    public AccountResponse createAccount(String userId, CreateAccountRequest request) {
        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .userId(userId)
                .balance(request.getInitialDeposit() != null ?
                        request.getInitialDeposit() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "CAD")
                .accountType(request.getAccountType())
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        return modelMapper.map(saved, AccountResponse.class);
    }

    @Override
    public AccountResponse getAccountById(String accountId) {
        Account account = accountRepository.findById(accountId) //todo change it to get account by account number not id
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        return modelMapper.map(account, AccountResponse.class);
    }

    @Override
    public List<AccountResponse> getAccountsByUserId(String userId) {
        return accountRepository.findAllByUserId(userId)
                .stream()
                .map(account -> modelMapper.map(account, AccountResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public AccountResponse getBalance(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .build();
    }

    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis() +
                (int)(Math.random() * 1000);
    }
    @Override
    public AccountResponse updateAccountStatus(String accountId, AccountStatus status, String userId, boolean isAdmin) throws UnauthorizedException {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (!isAdmin && !account.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't own this account");
        }

        account.setStatus(status);
        accountRepository.save(account);
        return modelMapper.map(account, AccountResponse.class);
    }
}
