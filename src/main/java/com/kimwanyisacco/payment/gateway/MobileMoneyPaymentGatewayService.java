package com.kimwanyisacco.payment.gateway;

import com.kimwanyisacco.payment.gateway.PaymentGatewayResult;

import java.math.BigDecimal;

/**
 * Abstraction over a real Mobile Money gateway (MTN MoMo API, Airtel Money
 * API, or an aggregator like Flutterwave/Pesapal). Swap the implementation
 * bean for a real one when ready - nothing above this interface needs to
 * change.
 */
public interface MobileMoneyPaymentGatewayService {

    PaymentGatewayResult charge(String phoneNumber, String provider, BigDecimal amount, String narration);
}
