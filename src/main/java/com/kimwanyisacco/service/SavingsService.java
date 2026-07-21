package com.kimwanyisacco.service;

import com.kimwanyisacco.dto.request.DepositRequest;
import com.kimwanyisacco.dto.request.WithdrawalRequest;
import com.kimwanyisacco.dto.response.SavingsAccountResponse;



import java.util.List;

public interface SavingsService {

    SavingsAccountResponse getAccountByMemberId(Long memberId);

    SavingsAccountResponse deposit(DepositRequest request, Long processedByAdminId);

    /**
     * Credits an account as the result of a confirmed Mobile Money/Card
     * payment initiated by the member themselves (no teller involved).
     * Ownership (that the account belongs to the paying member) must be
     * verified by the caller (PaymentService) before invoking this.
     */
    SavingsAccountResponse creditFromPayment(Long accountId, java.math.BigDecimal amount, String paymentReference);

    SavingsAccountResponse withdraw(WithdrawalRequest request, Long processedByAdminId);

    /** Applies the 5%-p.a.-monthly interest rule to a single account. Intended to be run by a scheduled job. */
    void applyMonthlyInterest(Long accountId);

    List<SavingsAccountResponse> getAllAccounts();
}
