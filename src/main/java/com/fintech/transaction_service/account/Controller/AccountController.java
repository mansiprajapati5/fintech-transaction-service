package com.fintech.transaction_service.account.Controller;

import com.fintech.transaction_service.account.Decorator.AccountResponse;
import com.fintech.transaction_service.account.Decorator.CreateAccountRequest;
import com.fintech.transaction_service.account.Enum.AccountStatus;
import com.fintech.transaction_service.account.Service.AccountService;
import com.fintech.transaction_service.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(userId, request));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @GetMapping("/{accountId}/balance")  //todo need to change the response that will just give balance
    public ResponseEntity<AccountResponse> getBalance(
            @PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getBalance(accountId));
    }

    @PatchMapping("/{accountId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable String accountId,
            @RequestParam AccountStatus status) throws UnauthorizedException {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ResponseEntity.ok(
                accountService.updateAccountStatus(accountId, status, userId, false));
    }
}