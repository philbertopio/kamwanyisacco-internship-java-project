package com.kimwanyisacco.service;

import com.kimwanyisacco.dto.response.MemberTransactionApprovalResponse;

import java.math.BigDecimal;
import java.util.List;

public interface MemberTransactionApprovalService {

    void requestSavingsDeposit(Long memberId, Long savingsAccountId, BigDecimal amount);

    void requestSavingsWithdrawal(Long memberId, Long savingsAccountId, BigDecimal amount);

    void requestLoanRepayment(Long memberId, Long loanId, BigDecimal amount);

    List<MemberTransactionApprovalResponse> getPendingRequests();

    void approve(Long requestId, Long adminId);

    void reject(Long requestId, Long adminId);
}
