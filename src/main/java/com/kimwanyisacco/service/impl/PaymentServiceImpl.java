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
import com.kimwanyisacco.repository.LoanRepository;
import com.kimwanyisacco.repository.MemberRepository;
import com.kimwanyisacco.repository.PaymentRepository;
import com.kimwanyisacco.repository.SavingsAccountRepository;
import com.kimwanyisacco.service.LoanService;
import com.kimwanyisacco.service.PaymentService;
import com.kimwanyisacco.service.SavingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    private final SavingsService savingsService;
    private final LoanService loanService;
    private final MobileMoneyPaymentGatewayService mobileMoneyGateway;
    private final CardPaymentGatewayService cardGateway;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              MemberRepository memberRepository,
                              SavingsAccountRepository savingsAccountRepository,
                              LoanRepository loanRepository,
                              SavingsService savingsService,
                              LoanService loanService,
                              MobileMoneyPaymentGatewayService mobileMoneyGateway,
                              CardPaymentGatewayService cardGateway) {
        this.paymentRepository = paymentRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.loanRepository = loanRepository;
        this.savingsService = savingsService;
        this.loanService = loanService;
        this.mobileMoneyGateway = mobileMoneyGateway;
        this.cardGateway = cardGateway;
    }

    @Override
    @Transactional
    public PaymentResponse payWithMobileMoney(MobileMoneyPaymentRequest request, Long payingMemberId) {
        Member member = memberRepository.findById(payingMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", payingMemberId));

        verifyOwnership(request.getPurpose(), request.getTargetId(), payingMemberId);

        Payment payment = Payment.builder()
                .member(member)
                .purpose(request.getPurpose())
                .method(PaymentMethod.MOBILE_MONEY)
                .amount(request.getAmount())
                .phoneNumber(request.getPhoneNumber())
                .mobileProvider(request.getMobileProvider())
                .status(PaymentStatus.INITIATED)
                .initiatedAt(LocalDateTime.now())
                .build();
        setTarget(payment, request.getPurpose(), request.getTargetId());
        payment = paymentRepository.save(payment);

        PaymentGatewayResult result = mobileMoneyGateway.charge(
                request.getPhoneNumber(), request.getMobileProvider(), request.getAmount(),
                "Kimwanyi SACCO - " + request.getPurpose());

        return finalizePayment(payment, result);
    }

    @Override
    @Transactional
    public PaymentResponse payWithCard(CardPaymentRequest request, Long payingMemberId) {
        Member member = memberRepository.findById(payingMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", payingMemberId));

        verifyOwnership(request.getPurpose(), request.getTargetId(), payingMemberId);
        validateExpiry(request.getExpiryMonth(), request.getExpiryYear());

        Payment payment = Payment.builder()
                .member(member)
                .purpose(request.getPurpose())
                .method(PaymentMethod.CARD)
                .amount(request.getAmount())
                .maskedCardNumber(maskCard(request.getCardNumber()))
                .cardScheme(request.getCardScheme())
                .status(PaymentStatus.INITIATED)
                .initiatedAt(LocalDateTime.now())
                .build();
        setTarget(payment, request.getPurpose(), request.getTargetId());
        payment = paymentRepository.save(payment);

        // NOTE: in a real integration the raw card number/CVV would go straight
        // to the gateway's tokenization endpoint, never held in a local variable
        // longer than necessary and never logged. This mock keeps the same
        // contract so swapping it later doesn't touch this method's shape.
        PaymentGatewayResult result = cardGateway.charge(
                payment.getMaskedCardNumber(), request.getCardScheme(), request.getAmount(),
                "Kimwanyi SACCO - " + request.getPurpose());

        return finalizePayment(payment, result);
    }

    @Override
    public List<PaymentResponse> getPaymentHistory(Long memberId) {
        return paymentRepository.findByMemberIdOrderByInitiatedAtDesc(memberId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Confirms the paying member actually owns the account/loan they're paying into. */
    private void verifyOwnership(PaymentPurpose purpose, Long targetId, Long payingMemberId) {
        if (purpose == PaymentPurpose.SAVINGS_DEPOSIT) {
            SavingsAccount account = savingsAccountRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("SavingsAccount", targetId));
            if (!account.getMember().getId().equals(payingMemberId)) {
                throw new PaymentProcessingException("You may only deposit into your own savings account");
            }
        } else if (purpose == PaymentPurpose.LOAN_REPAYMENT) {
            Loan loan = loanRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Loan", targetId));
            if (!loan.getMember().getId().equals(payingMemberId)) {
                throw new PaymentProcessingException("You may only repay your own loan");
            }
            if (loan.getStatus() != LoanStatus.ACTIVE) {
                throw new PaymentProcessingException("This loan is not currently active for repayment");
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

    private String maskCard(String cardNumber) {
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }

    /** Applies the gateway result: updates the Payment record, and on success, credits the account/loan. */
    private PaymentResponse finalizePayment(Payment payment, PaymentGatewayResult result) {
        payment.setStatus(result.getStatus());
        payment.setGatewayReference(result.getGatewayReference());
        payment.setFailureReason(result.getFailureReason());
        payment.setCompletedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        if (result.getStatus() == PaymentStatus.SUCCESSFUL) {
            if (payment.getPurpose() == PaymentPurpose.SAVINGS_DEPOSIT) {
                savingsService.creditFromPayment(payment.getSavingsAccountId(), payment.getAmount(),
                        payment.getGatewayReference());
            } else {
                LoanRepaymentRequest repaymentRequest = new LoanRepaymentRequest(payment.getLoanId(), payment.getAmount());
                loanService.repayLoan(repaymentRequest);
            }
        }

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
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
                .build();
    }
}
