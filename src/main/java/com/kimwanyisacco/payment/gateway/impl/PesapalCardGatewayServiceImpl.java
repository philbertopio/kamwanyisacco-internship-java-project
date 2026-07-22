package com.kimwanyisacco.payment.gateway.impl;

import com.kimwanyisacco.model.enums.PaymentStatus;
import com.kimwanyisacco.payment.gateway.CardPaymentGatewayService;
import com.kimwanyisacco.payment.gateway.PaymentGatewayResult;
import com.kimwanyisacco.payment.pesapal.PesapalGatewayService;
import com.kimwanyisacco.payment.pesapal.PesapalOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * PesaPal v3 card payment gateway implementation.
 *
 * <p>Because PesaPal hosts the checkout page, card tokenization happens on
 * PesaPal's PCI-DSS compliant servers — raw PAN and CVV are never routed
 * through this application. Only the masked card number and scheme are stored
 * locally for display purposes.
 *
 * <p>Returns {@link PaymentStatus#PENDING} and a {@code redirectUrl} — the
 * caller must open that URL so the member can enter their card details on
 * PesaPal's hosted iframe.
 */
@Service
public class PesapalCardGatewayServiceImpl implements CardPaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PesapalCardGatewayServiceImpl.class);

    private final PesapalGatewayService pesapal;

    @Autowired
    public PesapalCardGatewayServiceImpl(PesapalGatewayService pesapal) {
        this.pesapal = pesapal;
    }

    /**
     * @param maskedCardNumber Masked card (e.g. "**** **** **** 1234") — for local display only.
     *                         The real card number must NOT be passed here; it is entered by the
     *                         member directly on PesaPal's hosted page.
     * @param cardScheme       "VISA" or "MASTERCARD"
     * @param amount           Payment amount in UGX
     * @param narration        Order description (e.g. "Kimwanyi SACCO - SAVINGS_DEPOSIT")
     */
    @Override
    public PaymentGatewayResult charge(
            String maskedCardNumber,
            String cardScheme,
            BigDecimal amount,
            String narration) {

        // Generate a merchant reference from scheme + timestamp — will be overwritten
        // by PaymentServiceImpl with the proper per-attempt reference before submitting.
        // This method signature is kept compatible with the interface; the real reference
        // is injected by the service layer through the overloaded submitOrder call.
        log.info("Initiating PesaPal card payment: scheme={}, amount={}", cardScheme, amount);

        // Delegate to the shared gateway using narration as a placeholder.
        // PaymentServiceImpl.payWithCard() calls submitOrderDirect() instead for full context.
        // This path is only reached if someone calls the interface directly without member context.
        PesapalOrderResponse order = pesapal.submitOrder(
                generateFallbackRef(),
                amount,
                narration,
                "Card",         // firstName placeholder
                "Member",       // lastName placeholder
                "member@kimwanyisacco.com",
                null,
                "Kimwanyi SACCO"
        );

        return PaymentGatewayResult.builder()
                .status(PaymentStatus.PENDING)
                .gatewayReference(order.getOrderTrackingId())
                .orderTrackingId(order.getOrderTrackingId())
                .merchantReference(order.getMerchantReference())
                .redirectUrl(order.getRedirectUrl())
                .build();
    }

    private String generateFallbackRef() {
        return "CARD-" + Long.toHexString(System.currentTimeMillis()).toUpperCase();
    }
}
