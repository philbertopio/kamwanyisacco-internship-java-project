package com.kimwanyisacco.payment.gateway;

import com.kimwanyisacco.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Uniform result shape any gateway integration must produce.
 *
 * <p>For redirect-based gateways (e.g. PesaPal v3), the result will carry
 * {@code status = PENDING} plus a non-null {@code redirectUrl} that the
 * frontend must open so the member can complete payment on the hosted page.
 *
 * <p>For synchronous gateways the redirect fields will be null and status
 * will be {@code SUCCESSFUL} or {@code FAILED} immediately.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayResult {

    /** Final/interim payment status after the gateway call. */
    private PaymentStatus status;

    /** Gateway's own transaction reference (e.g. PesaPal order_tracking_id). */
    private String gatewayReference;

    /** Human-readable failure reason; null on success or pending. */
    private String failureReason;

    // ── Redirect-based flow (PesaPal v3) ─────────────────────────────────────

    /** PesaPal hosted checkout URL. Frontend must open this for the member. */
    private String redirectUrl;

    /** PesaPal's order_tracking_id — needed by the verify/poll endpoint. */
    private String orderTrackingId;

    /** Our merchant reference sent to PesaPal (unique per attempt). */
    private String merchantReference;
}
