package com.fintech.transaction_service.transaction.Controller;

import com.fintech.transaction_service.common.exception.InsufficientFundsException;
import com.fintech.transaction_service.common.exception.UnauthorizedException;
import com.fintech.transaction_service.transaction.Decorator.*;
import com.fintech.transaction_service.transaction.Service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request) throws UnauthorizedException {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.deposit(request, userId));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request) throws UnauthorizedException, InsufficientFundsException {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.withdraw(request, userId));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request) throws UnauthorizedException, InsufficientFundsException {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request, userId));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId));
    }

    @GetMapping("/history/{accountId}")
    public ResponseEntity<Page<TransactionResponse>> getHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                transactionService.getTransactionHistory(accountId, page, size));
    }
    @PostMapping("/payment")
    public ResponseEntity<TransactionResponse> payment(
            @Valid @RequestBody PaymentRequest request) throws UnauthorizedException, InsufficientFundsException {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.payment(request, userId));
    }
}