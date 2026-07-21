package com.kimwanyisacco.controller;


import com.kimwanyisacco.dto.request.LoanApplicationRequest;
import com.kimwanyisacco.dto.request.LoanDecisionRequest;
import com.kimwanyisacco.dto.request.LoanRepaymentRequest;
import com.kimwanyisacco.dto.response.ApiResponse;
import com.kimwanyisacco.dto.response.LoanResponse;
import com.kimwanyisacco.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;

    @Autowired
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LoanResponse>> apply(@Valid @RequestBody LoanApplicationRequest request) {
        LoanResponse response = loanService.applyForLoan(request);
        return new ResponseEntity<>(ApiResponse.of("Loan application submitted", response), HttpStatus.CREATED);
    }

    @PatchMapping("/{loanId}/decision")
    public ResponseEntity<ApiResponse<LoanResponse>> decide(@PathVariable Long loanId,
                                                            @Valid @RequestBody LoanDecisionRequest request) {
        LoanResponse response = loanService.decideLoan(loanId, request);
        return ResponseEntity.ok(ApiResponse.of("Loan decision recorded", response));
    }

    @PostMapping("/repay")
    public ResponseEntity<ApiResponse<LoanResponse>> repay(@Valid @RequestBody LoanRepaymentRequest request) {
        LoanResponse response = loanService.repayLoan(request);
        return ResponseEntity.ok(ApiResponse.of("Repayment recorded", response));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<ApiResponse<LoanResponse>> getById(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.of("Loan retrieved", loanService.getLoanById(loanId)));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getByMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(ApiResponse.of("Loans retrieved", loanService.getLoansByMember(memberId)));
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getOverdue() {
        return ResponseEntity.ok(ApiResponse.of("Overdue loans retrieved", loanService.getOverdueLoans()));
    }
}
