package com.kimwanyisacco.controller;

import com.kimwanyisacco.dto.request.CardPaymentRequest;
import com.kimwanyisacco.dto.request.MobileMoneyPaymentRequest;
import com.kimwanyisacco.dto.response.ApiResponse;
import com.kimwanyisacco.dto.response.PaymentResponse;
import com.kimwanyisacco.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * REST controller for PesaPal v3 payments.
 *
 * <h3>Typical client flow</h3>
 * <ol>
 *   <li>Call {@code POST /mobile-money} or {@code POST /card}
 *       → receive {@code status=PENDING} + {@code redirectUrl}</li>
 *   <li>Open {@code redirectUrl} in a new tab or iframe — member pays on PesaPal</li>
 *   <li>PesaPal redirects member back to your frontend (configured in .env as
 *       {@code PESAPAL_CALLBACK_URL}); parse {@code ?OrderMerchantReference=...}</li>
 *   <li>Poll {@code GET /verify/{merchantReference}} every 5 s until
 *       {@code status} is {@code SUCCESSFUL}, {@code FAILED}, or {@code CANCELLED}</li>
 * </ol>
 *
 * <p>PesaPal also calls {@code GET /ipn} asynchronously when the payment status changes.
 * No authentication on that endpoint — PesaPal cannot send a Bearer token.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INITIATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initiate a mobile money payment (MTN MoMo / Airtel Money).
     *
     * <p>Returns {@code status=PENDING} and a {@code redirectUrl} — the client
     * must open the URL so the member can approve the MoMo prompt on PesaPal's
     * hosted page.
     */
    @PostMapping("/mobile-money")
    public ResponseEntity<ApiResponse<PaymentResponse>> payWithMobileMoney(
            @Valid @RequestBody MobileMoneyPaymentRequest request,
            @RequestParam Long payingMemberId) {

        log.info("Mobile money payment request: member={}, amount={}, provider={}",
                 payingMemberId, request.getAmount(), request.getMobileProvider());

        PaymentResponse response = paymentService.payWithMobileMoney(request, payingMemberId);
        return ResponseEntity.status(201)
                .body(ApiResponse.of("Mobile money payment initiated. Open redirectUrl to pay.",
                        response));
    }

    /**
     * Initiate a card payment (Visa / Mastercard).
     *
     * <p>Returns {@code status=PENDING} and a {@code redirectUrl} — the client
     * must open the URL so the member can enter card details on PesaPal's
     * PCI-compliant hosted iframe. Raw card data never passes through this server.
     */
    @PostMapping("/card")
    public ResponseEntity<ApiResponse<PaymentResponse>> payWithCard(
            @Valid @RequestBody CardPaymentRequest request,
            @RequestParam Long payingMemberId) {

        log.info("Card payment request: member={}, amount={}, scheme={}",
                 payingMemberId, request.getAmount(), request.getCardScheme());

        PaymentResponse response = paymentService.payWithCard(request, payingMemberId);
        return ResponseEntity.status(201)
                .body(ApiResponse.of("Card payment initiated. Open redirectUrl to pay.", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY (frontend polling)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Poll this endpoint after initiating a payment.
     *
     * <ul>
     *   <li>Poll every <strong>5 seconds</strong> until {@code status} is
     *       {@code SUCCESSFUL}, {@code FAILED}, or {@code CANCELLED}.</li>
     *   <li>Also call this from the callback page after PesaPal redirects the
     *       member back using {@code ?OrderMerchantReference=...}</li>
     * </ul>
     */
    @GetMapping("/verify/{merchantReference}")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @PathVariable String merchantReference) {

        log.debug("Verify payment request: merchantReference={}", merchantReference);
        PaymentResponse response = paymentService.verifyPayment(merchantReference);
        return ResponseEntity.ok(ApiResponse.of("Payment status: " + response.getStatus(), response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IPN (PesaPal callback — no auth)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * PesaPal Instant Payment Notification (IPN) endpoint.
     *
     * <p>PesaPal sends a GET request here whenever a payment status changes.
     * <strong>No JWT authentication</strong> — PesaPal cannot send a Bearer token.
     * Security: PesaPal only calls IPN URLs registered through its API.
     *
     * <p>Configure in {@code .env}:
     * <pre>
     * PESAPAL_IPN_URL=https://your-domain.com/api/v1/payments/ipn
     * </pre>
     *
     * <p>For local dev, expose port 8080 with ngrok:
     * <pre>
     * ngrok http 8080
     * # then set PESAPAL_IPN_URL=https://xxxx.ngrok.io/api/v1/payments/ipn
     * </pre>
     */
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, Object>> paymentIpn(
            @RequestParam("OrderTrackingId")        String orderTrackingId,
            @RequestParam("OrderMerchantReference") String merchantReference,
            @RequestParam(value = "OrderNotificationType", defaultValue = "IPNCHANGE")
                    String notificationType) {

        log.info("PesaPal IPN: trackingId={} merchantRef={} type={}",
                 orderTrackingId, merchantReference, notificationType);

        Map<String, Object> ack = paymentService.processIpn(orderTrackingId, merchantReference);
        return ResponseEntity.ok(ack);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HISTORY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all payment attempts for a member, newest first.
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> paymentHistory(
            @PathVariable Long memberId) {

        return ResponseEntity.ok(ApiResponse.of("Payment history retrieved",
                paymentService.getPaymentHistory(memberId)));
    }
}
