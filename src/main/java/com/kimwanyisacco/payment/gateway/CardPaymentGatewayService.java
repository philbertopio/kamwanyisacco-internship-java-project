package com.kimwanyisacco.payment.gateway;

import com.kimwanyisacco.payment.gateway.PaymentGatewayResult;

import java.math.BigDecimal;

/**
 * Abstraction over a real Card gateway (Stripe, Flutterwave, Pesapal, DPO).
 * The real implementation should never receive/store the raw PAN or CVV in
 * application logs - route those directly to the gateway's hosted
 * field/tokenization flow instead of through this method signature as-is.
 */
public interface CardPaymentGatewayService {

    PaymentGatewayResult charge(String maskedCardNumber, String cardScheme, BigDecimal amount, String narration);
}
