package com.fintech.transaction_service.transaction.Service;

import com.fintech.transaction_service.common.exception.InsufficientFundsException;
import com.fintech.transaction_service.common.exception.UnauthorizedException;
import com.fintech.transaction_service.transaction.Decorator.DepositRequest;
import com.fintech.transaction_service.transaction.Decorator.TransactionResponse;
import com.fintech.transaction_service.transaction.Decorator.TransferRequest;
import com.fintech.transaction_service.transaction.Decorator.WithdrawRequest;
import org.springframework.data.domain.Page;

public interface TransactionService {
    TransactionResponse deposit(DepositRequest request, String userId) throws UnauthorizedException;
    TransactionResponse withdraw(WithdrawRequest request, String userId) throws UnauthorizedException, InsufficientFundsException;
    TransactionResponse transfer(TransferRequest request, String userId) throws UnauthorizedException, InsufficientFundsException;
    TransactionResponse getTransactionById(String transactionId);
    Page<TransactionResponse> getTransactionHistory(String accountId, int page, int size);
}
