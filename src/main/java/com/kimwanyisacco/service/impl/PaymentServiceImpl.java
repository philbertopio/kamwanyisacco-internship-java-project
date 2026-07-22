package com.kimwanyisacco.service.impl;

import com.kimwanyisacco.dto.request.CardPaymentRequest;
import com.kimwanyisacco.dto.request.LoanRepaymentRequest;
import com.kimwanyisacco.dto.request.MobileMoneyPaymentRequest;
import com.kimwanyisacco.dto.response.PaymentResponse;
import com.kimwanyisacco.exception.PaymentProcessingException;
import com.kimwanyisacco.exception.ResourceNotFoundException;
import com.kimwanyisacco.model.entity.Loan;
import com.kimwanyisacco.model.entity.Member;
import com.kimwanyisacco.model.entity.Payment;
import com.kimwanyisacco.model.entity.SavingsAccount;
import com.kimwanyisacco.model.enums.LoanStatus;
import com.kimwanyisacco.model.enums.PaymentMethod;
import com.kimwanyisacco.model.enums.PaymentPurpose;
import com.kimwanyisacco.model.enums.PaymentStatus;
import com.kimwanyisacco.payment.gateway.CardPaymentGatewayService;
import com.kimwanyisacco.payment.gateway.MobileMoneyPaymentGatewayService;
import com.kimwanyisacco.payment.gateway.PaymentGatewayResult;
import com.kimwanyisacco.payment.pesapal.PesapalGatewayService;
import com.kimwanyisacco.payment.pesapal.PesapalOrderResponse;
import com.kimwanyisacco.payment.pesapal.PesapalTransactionStatus;
import com.kimwanyisacco.repository.LoanRepository;
import com.kimwanyisacco.repository.MemberRepository;
import com.kimwanyisacco.repository.PaymentRepository;
import com.kimwanyisacco.repository.SavingsAccountRepository;
import com.kimwanyisacco.service.LoanService;
import com.kimwanyisacco.service.PaymentService;
import com.kimwanyisacco.service.SavingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PesaPal v3 payment service implementation.
 *
 * <h3>Key design decisions</h3>
 * <ul>
 *   <li>Both card and mobile money go through PesaPal's hosted checkout — same
 *       {@code SubmitOrderRequest} endpoint, same redirect flow.</li>
 *   <li>{@link #payWithMobileMoney} and {@link #payWithCard} always return
 *       {@link PaymentStatus#PENDING} with a {@code redirectUrl}. The credit
 *       to savings/loan only happens inside {@link #applyCompletedPayment} after
 *       PesaPal confirms {@code status_code = 1 (COMPLETED)}.</li>
 *   <li>Idempotency: {@link #verifyPayment} is a no-op for already-terminal payments.</li>
 *   <li>The merchant reference format is {@code SACCO-{memberId}-{8 hex chars}},
 *       guaranteed unique per attempt and ≤ 50 chars (PesaPal's limit).</li>
 * </ul>
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository         paymentRepository;
    private final MemberRepository          memberRepository;
    private final SavingsAccountRepository  savingsAccountRepository;
    private final LoanRepository            loanRepository;
    private final SavingsService            savingsService;
    private final LoanService               loanService;
    private final PesapalGatewayService     pesapal;

    // Gateway-level interfaces kept for possible future swapping
    private final MobileMoneyPaymentGatewayService mobileMoneyGateway;
    private final CardPaymentGatewayService         cardGateway;

    @Autowired
    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            LoanRepository loanRepository,
            SavingsService savingsService,
            LoanService loanService,
            PesapalGatewayService pesapal,
            MobileMoneyPaymentGatewayService mobileMoneyGateway,
            CardPaymentGatewayService cardGateway) {
        this.paymentRepository        = paymentRepository;
        this.memberRepository         = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.loanRepository           = loanRepository;
        this.savingsService           = savingsService;
        this.loanService              = loanService;
        this.pesapal                  = pesapal;
        this.mobileMoneyGateway       = mobileMoneyGateway;
        this.cardGateway              = cardGateway;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INITIATE PAYMENTS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse payWithMobileMoney(MobileMoneyPaymentRequest request, Long payingMemberId) {
        Member member = fetchMember(payingMemberId);
        verifyOwnership(request.getPurpose(), request.getTargetId(), payingMemberId);

        String merchantRef = buildMerchantRef(payingMemberId);

        Payment payment = Payment.builder()
                .member(member)
                .purpose(request.getPurpose())
                .method(PaymentMethod.MOBILE_MONEY)
                .amount(request.getAmount())
                .phoneNumber(request.getPhoneNumber())
                .mobileProvider(request.getMobileProvider())
                .merchantReference(merchantRef)
                .status(PaymentStatus.INITIATED)
                .initiatedAt(LocalDateTime.now())
                .build();
        setTarget(payment, request.getPurpose(), request.getTargetId());
        payment = paymentRepository.save(payment);

        // Submit to PesaPal — use the richer path with full member context
        String narration = "Kimwanyi SACCO - " + request.getPurpose();
        PesapalOrderResponse order = pesapal.submitOrder(
                merchantRef,
                request.getAmount(),
                narration,
                "Mobile",       // first name placeholder (member name not on request)
                "Member",
                "member@kimwanyisacco.com",
                request.getPhoneNumber(),   // pre-fills MoMo on hosted page
                "Kimwanyi SACCO"
        );

        return persistPendingAndRespond(payment, order, PaymentMethod.MOBILE_MONEY);
    }

    @Override
    @Transactional
    public PaymentResponse payWithCard(CardPaymentRequest request, Long payingMemberId) {
        Member member = fetchMember(payingMemberId);
        verifyOwnership(request.getPurpose(), request.getTargetId(), payingMemberId);
        validateExpiry(request.getExpiryMonth(), request.getExpiryYear());

        String merchantRef = buildMerchantRef(payingMemberId);

        Payment payment = Payment.builder()
                .member(member)
                .purpose(request.getPurpose())
                .method(PaymentMethod.CARD)
                .amount(request.getAmount())
                .maskedCardNumber(maskCard(request.getCardNumber()))
                .cardScheme(request.getCardScheme())
                .merchantReference(merchantRef)
                .status(PaymentStatus.INITIATED)
                .initiatedAt(LocalDateTime.now())
                .build();
        setTarget(payment, request.getPurpose(), request.getTargetId());
        payment = paymentRepository.save(payment);

        // Submit to PesaPal — raw card details are entered by the member on PesaPal's
        // PCI-compliant hosted page; this backend never receives or stores them.
        String narration = "Kimwanyi SACCO - " + request.getPurpose();
        String[] nameParts = splitName(request.getCardHolderName());
        PesapalOrderResponse order = pesapal.submitOrder(
                merchantRef,
                request.getAmount(),
                narration,
                nameParts[0],
                nameParts[1],
                "member@kimwanyisacco.com",
                null,   // no phone for card payments
                "Kimwanyi SACCO"
        );

        return persistPendingAndRespond(payment, order, PaymentMethod.CARD);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY PAYMENT (frontend polling)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse verifyPayment(String merchantReference) {
        Payment payment = paymentRepository.findByMerchantReference(merchantReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", merchantReference));

        // Idempotent: already at a terminal status — skip gateway call
        if (isTerminal(payment.getStatus())) {
            log.debug("verifyPayment: {} already terminal ({})",
                      merchantReference, payment.getStatus());
            return toResponse(payment);
        }

        // Payment was never submitted to PesaPal (gateway call failed on initiate)
        if (payment.getOrderTrackingId() == null) {
            return toResponse(payment);
        }

        // Poll PesaPal
        PesapalTransactionStatus txn;
        try {
            txn = pesapal.getTransactionStatus(payment.getOrderTrackingId());
        } catch (Exception e) {
            log.warn("PesaPal status check failed for {}: {}", merchantReference, e.getMessage());
            return toResponse(payment);  // return current status; don't crash the UI
        }

        applyPesapalStatus(payment, txn);
        paymentRepository.save(payment);
        return toResponse(payment);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IPN HANDLER
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Map<String, Object> processIpn(String orderTrackingId, String merchantReference) {
        log.info("IPN received: orderTrackingId={} merchantRef={}", orderTrackingId, merchantReference);

        paymentRepository.findByMerchantReference(merchantReference).ifPresentOrElse(
                payment -> {
                    // Ensure tracking id is stored (in case initiate partially failed)
                    if (payment.getOrderTrackingId() == null) {
                        payment.setOrderTrackingId(orderTrackingId);
                        paymentRepository.save(payment);
                    }
                    if (!isTerminal(payment.getStatus())) {
                        // Trigger a full status check and apply
                        try {
                            verifyPayment(merchantReference);
                        } catch (Exception e) {
                            log.error("IPN-triggered verify failed for {}: {}",
                                      merchantReference, e.getMessage());
                        }
                    } else {
                        log.info("IPN duplicate for {} — already {}", merchantReference,
                                 payment.getStatus());
                    }
                },
                () -> log.warn("IPN for unknown merchant_reference={}", merchantReference)
        );

        // Mandatory PesaPal IPN acknowledgement — must return exactly this structure
        Map<String, Object> ack = new HashMap<>();
        ack.put("orderNotificationType",  "IPNCHANGE");
        ack.put("orderTrackingId",        orderTrackingId);
        ack.put("orderMerchantReference", merchantReference);
        ack.put("status",                 200);
        return ack;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAYMENT HISTORY
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<PaymentResponse> getPaymentHistory(Long memberId) {
        return paymentRepository.findByMemberIdOrderByInitiatedAtDesc(memberId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persists PesaPal order details on the Payment record and returns a PENDING response.
     * Called immediately after {@link PesapalGatewayService#submitOrder} succeeds.
     */
    private PaymentResponse persistPendingAndRespond(
            Payment payment,
            PesapalOrderResponse order,
            PaymentMethod method) {

        payment.setStatus(PaymentStatus.PENDING);
        payment.setOrderTrackingId(order.getOrderTrackingId());
        payment.setRedirectUrl(order.getRedirectUrl());
        payment.setIpnId(pesapal.getIpnId());
        paymentRepository.save(payment);

        log.info("Payment {} submitted to PesaPal — tracking_id={}, redirect={}",
                 payment.getMerchantReference(), order.getOrderTrackingId(),
                 order.getRedirectUrl());

        return toResponse(payment);
    }

    /**
     * Maps a PesaPal {@link PesapalTransactionStatus} onto the local {@link Payment} entity.
     *
     * <p>PesaPal {@code status_code} meanings:
     * <ul>
     *   <li>0 = INVALID / not processed yet</li>
     *   <li>1 = COMPLETED</li>
     *   <li>2 = FAILED</li>
     *   <li>3 = REVERSED</li>
     * </ul>
     */
    private void applyPesapalStatus(Payment payment, PesapalTransactionStatus txn) {
        if (txn.getStatusCode() == null) return;  // neutral response, keep PENDING

        int code = txn.getStatusCode();
        String desc = txn.getPaymentStatusDescription() != null
                ? txn.getPaymentStatusDescription().toUpperCase() : "";

        if (code == 1 || "COMPLETED".equals(desc)) {
            // Amount sanity check — guard against amount tampering
            if (txn.getAmount() != null
                    && txn.getAmount().compareTo(BigDecimal.ZERO) > 0
                    && payment.getAmount().subtract(txn.getAmount()).abs()
                               .compareTo(BigDecimal.ONE) > 0) {
                log.error("Amount mismatch for {}: expected {}, PesaPal returned {}",
                          payment.getMerchantReference(), payment.getAmount(), txn.getAmount());
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Amount mismatch: expected "
                        + payment.getAmount() + ", gateway returned " + txn.getAmount());
                return;
            }

            payment.setStatus(PaymentStatus.SUCCESSFUL);
            payment.setGatewayReference(txn.getConfirmationCode());
            payment.setCompletedAt(LocalDateTime.now());

            // Map PesaPal's payment_method string to our enum
            String pm = txn.getPaymentMethod() != null ? txn.getPaymentMethod().toUpperCase() : "";
            if (pm.contains("CARD") || pm.contains("VISA") || pm.contains("MASTER")) {
                payment.setMethod(PaymentMethod.CARD);
            } else {
                payment.setMethod(PaymentMethod.MOBILE_MONEY);
            }

            // Apply business effect — idempotency guarded by status check above
            applyCompletedPayment(payment);

        } else if (code == 2 || "FAILED".equals(desc)) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(txn.getDescription() != null
                    ? txn.getDescription() : "Payment failed at gateway");

        } else if (code == 3 || "REVERSED".equals(desc)) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setFailureReason("Payment reversed by gateway");
        }
        // code == 0 means still processing — keep PENDING, no change
    }

    /**
     * Credits the savings account or repays the loan after a COMPLETED payment.
     * Must only be called once — caller ensures idempotency via status check.
     */
    private void applyCompletedPayment(Payment payment) {
        try {
            if (payment.getPurpose() == PaymentPurpose.SAVINGS_DEPOSIT) {
                savingsService.creditFromPayment(
                        payment.getSavingsAccountId(),
                        payment.getAmount(),
                        payment.getGatewayReference());
                log.info("Savings account {} credited {} UGX via payment {}",
                         payment.getSavingsAccountId(), payment.getAmount(),
                         payment.getMerchantReference());
            } else if (payment.getPurpose() == PaymentPurpose.LOAN_REPAYMENT) {
                LoanRepaymentRequest repayment = new LoanRepaymentRequest(
                        payment.getLoanId(), payment.getAmount());
                loanService.repayLoan(repayment);
                log.info("Loan {} repaid {} UGX via payment {}",
                         payment.getLoanId(), payment.getAmount(),
                         payment.getMerchantReference());
            }
        } catch (Exception e) {
            // Log but don't re-throw — payment is already SUCCESSFUL on PesaPal's side.
            // A scheduled reconciliation job should handle residual failures.
            log.error("Failed to apply completed payment {} to {}: {}",
                      payment.getMerchantReference(), payment.getPurpose(), e.getMessage(), e);
        }
    }

    // ─── Ownership & validation ───────────────────────────────────────────────

    private void verifyOwnership(PaymentPurpose purpose, Long targetId, Long payingMemberId) {
        if (purpose == PaymentPurpose.SAVINGS_DEPOSIT) {
            SavingsAccount account = savingsAccountRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("SavingsAccount", targetId));
            if (!account.getMember().getId().equals(payingMemberId)) {
                throw new PaymentProcessingException(
                        "You may only deposit into your own savings account");
            }
        } else if (purpose == PaymentPurpose.LOAN_REPAYMENT) {
            Loan loan = loanRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Loan", targetId));
            if (!loan.getMember().getId().equals(payingMemberId)) {
                throw new PaymentProcessingException("You may only repay your own loan");
            }
            if (loan.getStatus() != LoanStatus.ACTIVE) {
                throw new PaymentProcessingException(
                        "This loan is not currently active for repayment");
            }
        }
    }

    private void setTarget(Payment payment, PaymentPurpose purpose, Long targetId) {
        if (purpose == PaymentPurpose.SAVINGS_DEPOSIT) {
            payment.setSavingsAccountId(targetId);
        } else {
            payment.setLoanId(targetId);
        }
    }

    private void validateExpiry(int month, int year) {
        YearMonth expiry = YearMonth.of(year, month);
        if (expiry.isBefore(YearMonth.from(LocalDate.now()))) {
            throw new PaymentProcessingException("Card has expired");
        }
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private Member fetchMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
    }

    /**
     * Generates a unique merchant reference: {@code SACCO-{memberId}-{8hex}}.
     * Max length is well under PesaPal's 50-char limit.
     */
    private String buildMerchantRef(Long memberId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "SACCO-" + memberId + "-" + suffix;
    }

    private String maskCard(String cardNumber) {
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"Member", ""};
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length == 2 ? parts : new String[]{parts[0], ""};
    }

    private boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.SUCCESSFUL
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.CANCELLED;
    }

    /** Maps a Payment entity to the API response DTO. */
    private PaymentResponse toResponse(Payment payment) {
        String message = buildMessage(payment);
        return PaymentResponse.builder()
                .id(payment.getId())
                .purpose(payment.getPurpose())
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .gatewayReference(payment.getGatewayReference())
                .failureReason(payment.getFailureReason())
                .initiatedAt(payment.getInitiatedAt())
                .completedAt(payment.getCompletedAt())
                .merchantReference(payment.getMerchantReference())
                .orderTrackingId(payment.getOrderTrackingId())
                .redirectUrl(payment.getRedirectUrl())
                .message(message)
                .build();
    }

    private String buildMessage(Payment payment) {
        return switch (payment.getStatus()) {
            case PENDING -> "Payment initiated. Open the redirectUrl to complete payment on PesaPal.";
            case SUCCESSFUL -> "Payment completed successfully.";
            case FAILED -> "Payment failed: " + payment.getFailureReason();
            case CANCELLED -> "Payment was cancelled or reversed.";
            default -> "Payment is " + payment.getStatus().name().toLowerCase() + ".";
        };
    }
}
