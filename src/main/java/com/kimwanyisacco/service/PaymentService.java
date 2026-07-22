package com.kimwanyisacco.service;

import com.kimwanyisacco.dto.request.CardPaymentRequest;
import com.kimwanyisacco.dto.request.MobileMoneyPaymentRequest;
import com.kimwanyisacco.dto.response.PaymentResponse;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates PesaPal v3 payments: validates ownership, records the attempt,
 * calls the gateway, and — on COMPLETED — applies the result to the member's
 * savings account or loan.
 *
 * <h3>PesaPal flow</h3>
 * <ol>
 *   <li>{@link #payWithMobileMoney} / {@link #payWithCard} — submit order, return redirect URL</li>
 *   <li>Member pays on PesaPal hosted page</li>
 *   <li>PesaPal calls {@link #processIpn} (IPN endpoint)</li>
 *   <li>Frontend polls {@link #verifyPayment} every 5 s until terminal status</li>
 * </ol>
 */
public interface PaymentService {

    /** Initiate a mobile money payment via PesaPal. Returns PENDING + redirectUrl. */
    PaymentResponse payWithMobileMoney(MobileMoneyPaymentRequest request, Long payingMemberId);

    /** Initiate a card payment via PesaPal. Returns PENDING + redirectUrl. */
    PaymentResponse payWithCard(CardPaymentRequest request, Long payingMemberId);

    /**
     * Poll PesaPal for the current status of a payment by merchant reference.
     * Applies savings/loan credit when the status becomes COMPLETED.
     * Safe to call repeatedly — idempotent after reaching a terminal status.
     */
    PaymentResponse verifyPayment(String merchantReference);

    /**
     * Handle a PesaPal IPN GET notification.
     * Returns the mandatory PesaPal acknowledgement JSON map.
     * No authentication — PesaPal cannot send a Bearer token.
     */
    Map<String, Object> processIpn(String orderTrackingId, String merchantReference);

    /** Full payment history for a member, newest first. */
    List<PaymentResponse> getPaymentHistory(Long memberId);
}
