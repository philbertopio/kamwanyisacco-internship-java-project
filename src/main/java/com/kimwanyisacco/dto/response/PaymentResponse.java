package com.kimwanyisacco.dto.response;


import com.kimwanyisacco.model.enums.PaymentMethod;
import com.kimwanyisacco.model.enums.PaymentPurpose;
import com.kimwanyisacco.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private PaymentPurpose purpose;
    private PaymentMethod method;
    private BigDecimal amount;
    private PaymentStatus status;
    private String gatewayReference;
    private String failureReason;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
}
