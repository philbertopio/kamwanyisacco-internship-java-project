package com.kimwanyisacco.service;

import com.kimwanyisacco.dto.request.LoanApplicationRequest;
import com.kimwanyisacco.dto.request.LoanDecisionRequest;
import com.kimwanyisacco.dto.request.LoanRepaymentRequest;
import com.kimwanyisacco.dto.response.LoanResponse;

import java.util.List;

public interface LoanService {

    LoanResponse applyForLoan(LoanApplicationRequest request);

    LoanResponse decideLoan(Long loanId, LoanDecisionRequest request);

    LoanResponse repayLoan(LoanRepaymentRequest request);

    LoanResponse getLoanById(Long loanId);

    List<LoanResponse> getLoansByMember(Long memberId);

    List<LoanResponse> getOverdueLoans();

    /** Powers the admin loan-review screen. */
    List<LoanResponse> getPendingLoans();
}

