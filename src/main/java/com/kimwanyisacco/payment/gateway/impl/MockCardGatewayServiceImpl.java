package com.kimwanyisacco.payment.gateway.impl;


import com.kimwanyisacco.model.enums.PaymentStatus;
import com.kimwanyisacco.payment.gateway.CardPaymentGatewayService;
import com.kimwanyisacco.payment.gateway.PaymentGatewayResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * TEMPORARY simulated Card gateway - see MockMobileMoneyGatewayServiceImpl
 * for the same "replace me" note. A real implementation should use the
 * gateway's hosted fields/tokenization so raw card data never transits or
 * is logged by this application at all.
 */
@Service
public class MockCardGatewayServiceImpl implements CardPaymentGatewayService {

    @Override
    public PaymentGatewayResult charge(String maskedCardNumber, String cardScheme, BigDecimal amount, String narration) {
        String reference = "CARD-" + cardScheme + "-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        return PaymentGatewayResult.builder()
                .status(PaymentStatus.SUCCESSFUL)
                .gatewayReference(reference)
                .build();
    }
}
