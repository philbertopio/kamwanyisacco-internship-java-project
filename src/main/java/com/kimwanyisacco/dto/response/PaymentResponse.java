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

    // ── PesaPal v3 redirect fields ────────────────────────────────────────────

    /** Unique reference for this payment attempt. Use to poll /verify/{ref}. */
    private String merchantReference;

    /** PesaPal's order tracking id (for internal use / debugging). */
    private String orderTrackingId;

    /**
     * PesaPal hosted checkout URL.
     * Non-null when status=PENDING — frontend must open this so the member
     * can choose their payment method (MTN MoMo, Airtel, Visa, Mastercard).
     */
    private String redirectUrl;

    /** Human-readable status message for the frontend to display. */
    private String message;
}
