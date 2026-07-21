package com.kimwanyisacco.service;



import com.kimwanyisacco.dto.request.CardPaymentRequest;
import com.kimwanyisacco.dto.request.MobileMoneyPaymentRequest;
import com.kimwanyisacco.dto.response.PaymentResponse;

import java.util.List;

/**
 * Orchestrates real-time Mobile Money / Card payments: validates ownership,
 * records the attempt, calls the gateway, and - on success - applies the
 * result to the member's savings account or loan.
 */
public interface PaymentService {

    PaymentResponse payWithMobileMoney(MobileMoneyPaymentRequest request, Long payingMemberId);

    PaymentResponse payWithCard(CardPaymentRequest request, Long payingMemberId);

    List<PaymentResponse> getPaymentHistory(Long memberId);
}
