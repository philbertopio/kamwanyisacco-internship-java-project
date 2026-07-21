package com.kimwanyisacco.controller;

import com.kimwanyisacco.dto.request.DepositRequest;
import com.kimwanyisacco.dto.request.WithdrawalRequest;
import com.kimwanyisacco.dto.response.ApiResponse;
import com.kimwanyisacco.dto.response.SavingsAccountResponse;

import com.kimwanyisacco.service.SavingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/savings")
public class SavingsController {

    private final SavingsService savingsService;

    @Autowired
    public SavingsController(SavingsService savingsService) {
        this.savingsService = savingsService;
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<SavingsAccountResponse>> getByMember(@PathVariable Long memberId) {
        return okResponse("Account retrieved", savingsService.getAccountByMemberId(memberId));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavingsAccountResponse>>> getAll() {
        return okResponse("Accounts retrieved", savingsService.getAllAccounts());
    }

    // processedByAdminId represents the cashier/admin performing the transaction.
    // In a full implementation this would be pulled from the authenticated
    // principal rather than a request param.
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<SavingsAccountResponse>> deposit(
            @Valid @RequestBody DepositRequest request,
            @RequestParam Long processedByAdminId) {
        return okResponse("Deposit successful", savingsService.deposit(request, processedByAdminId));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<SavingsAccountResponse>> withdraw(
            @Valid @RequestBody WithdrawalRequest request,
            @RequestParam Long processedByAdminId) {
        return okResponse("Withdrawal successful", savingsService.withdraw(request, processedByAdminId));
    }

    private <T> ResponseEntity<ApiResponse<T>> okResponse(String message, T data) {
        return ResponseEntity.ok(ApiResponse.of(message, data));
    }
}
