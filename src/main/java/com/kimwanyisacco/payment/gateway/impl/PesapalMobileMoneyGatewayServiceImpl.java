package com.kimwanyisacco.payment.gateway.impl;

import com.kimwanyisacco.model.enums.PaymentStatus;
import com.kimwanyisacco.payment.gateway.MobileMoneyPaymentGatewayService;
import com.kimwanyisacco.payment.gateway.PaymentGatewayResult;
import com.kimwanyisacco.payment.pesapal.PesapalGatewayService;
import com.kimwanyisacco.payment.pesapal.PesapalOrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * PesaPal v3 mobile money gateway implementation.
 *
 * <p>PesaPal's hosted checkout page handles both MTN MoMo and Airtel Money.
 * The phone number passed here is sent in the {@code billing_address} so
 * PesaPal can pre-fill the mobile money prompt for a faster checkout experience.
 *
 * <p>Returns {@link PaymentStatus#PENDING} and a {@code redirectUrl} — the
 * caller must open that URL so the member can approve the MoMo push notification
 * or enter their PIN on PesaPal's hosted page.
 */
@Service
public class PesapalMobileMoneyGatewayServiceImpl implements MobileMoneyPaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(PesapalMobileMoneyGatewayServiceImpl.class);

    private final PesapalGatewayService pesapal;

    @Autowired
    public PesapalMobileMoneyGatewayServiceImpl(PesapalGatewayService pesapal) {
        this.pesapal = pesapal;
    }

    /**
     * @param phoneNumber Mobile money number (e.g. "0771234567" or "+256771234567")
     * @param provider    "MTN" or "AIRTEL" — informational; PesaPal detects from phone prefix
     * @param amount      Payment amount in UGX
     * @param narration   Order description (e.g. "Kimwanyi SACCO - LOAN_REPAYMENT")
     */
    @Override
    public PaymentGatewayResult charge(
            String phoneNumber,
            String provider,
            BigDecimal amount,
            String narration) {

        log.info("Initiating PesaPal MoMo payment: provider={}, phone={}, amount={}",
                 provider, phoneNumber, amount);

        PesapalOrderResponse order = pesapal.submitOrder(
                generateFallbackRef(provider),
                amount,
                narration,
                "Mobile",       // firstName placeholder
                "Member",       // lastName placeholder
                "member@kimwanyisacco.com",
                phoneNumber,    // pre-fills MoMo prompt on PesaPal's hosted page
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

    private String generateFallbackRef(String provider) {
        return "MM-" + provider + "-" + Long.toHexString(System.currentTimeMillis()).toUpperCase();
    }
}
