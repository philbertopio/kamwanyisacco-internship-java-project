package com.kimwanyisacco.service;

import java.util.List;

public interface LoanService {

    LoanResponse applyForLoan(LoanApplicationRequest request);

    LoanResponse decideLoan(Long loanId, LoanDecisionRequest request);

    LoanResponse repayLoan(LoanRepaymentRequest request);

    LoanResponse getLoanById(Long loanId);

    List<LoanResponse> getLoansByMember(Long memberId);

    List<LoanResponse> getOverdueLoans();
}
