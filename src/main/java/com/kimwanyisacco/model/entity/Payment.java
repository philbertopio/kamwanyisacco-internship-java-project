package com.kimwanyisacco.model.entity;

import com.kimwanyisacco.model.enums.PaymentMethod;
import com.kimwanyisacco.model.enums.PaymentPurpose;
import com.kimwanyisacco.model.enums.PaymentStatus;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private PaymentPurpose purpose;

    // Set when purpose = SAVINGS_DEPOSIT
    @Column(name = "savings_account_id")
    private Long savingsAccountId;

    // Set when purpose = LOAN_REPAYMENT
    @Column(name = "loan_id")
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // Mobile Money fields (nullable - only populated when method = MOBILE_MONEY)
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "mobile_provider", length = 20)
    private String mobileProvider; // e.g. MTN, AIRTEL

    // Card fields (nullable - only populated when method = CARD). Never store full PAN/CVV.
    @Column(name = "masked_card_number", length = 25)
    private String maskedCardNumber;

    @Column(name = "card_scheme", length = 20)
    private String cardScheme; // e.g. VISA, MASTERCARD

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}