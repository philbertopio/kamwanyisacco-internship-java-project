package com.kimwanyisacco.payment.gateway;

import com.kimwanyisacco.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Uniform result shape any real gateway integration must produce. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayResult {
    private PaymentStatus status;
    private String gatewayReference;
    private String failureReason;
}
