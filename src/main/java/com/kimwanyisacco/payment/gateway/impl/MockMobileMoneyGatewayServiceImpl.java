package com.kimwanyisacco.payment.gateway.impl;


import com.kimwanyisacco.model.enums.PaymentStatus;
import com.kimwanyisacco.payment.gateway.MobileMoneyPaymentGatewayService;
import com.kimwanyisacco.payment.gateway.PaymentGatewayResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TEMPORARY simulated Mobile Money gateway. It always returns SUCCESSFUL
 * with a generated reference so the end-to-end flow (UI -> Payment record
 * -> savings/loan update) can be built, demoed, and tested today.
 *
 * TO GO LIVE: replace this bean with a real implementation that calls the
 * MTN MoMo Collections API or Airtel Money Open API (typically: request a
 * payment, poll or receive a webhook, then map the provider's status to
 * PaymentStatus). Keep this interface signature the same so nothing else
 * in the app has to change.
 */
@Service
public class MockMobileMoneyGatewayServiceImpl implements MobileMoneyPaymentGatewayService {

    @Override
    public PaymentGatewayResult charge(String phoneNumber, String provider, BigDecimal amount, String narration) {
        String reference = "MM-" + provider + "-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        return PaymentGatewayResult.builder()
                .status(PaymentStatus.SUCCESSFUL)
                .gatewayReference(reference)
                .build();
    }
}
