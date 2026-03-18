package com.fintech.transaction_service.account.Service;

import com.fintech.transaction_service.account.Decorator.AccountResponse;
import com.fintech.transaction_service.account.Decorator.CreateAccountRequest;
import com.fintech.transaction_service.account.Enum.AccountStatus;
import com.fintech.transaction_service.common.exception.UnauthorizedException;

import java.util.List;

public interface AccountService {
    AccountResponse createAccount(String userId, CreateAccountRequest request);
    AccountResponse getAccountById(String accountId);
    List<AccountResponse> getAccountsByUserId(String userId);
    AccountResponse getBalance(String accountId);
    AccountResponse updateAccountStatus(String accountId, AccountStatus status, String userId, boolean isAdmin) throws UnauthorizedException;
}
