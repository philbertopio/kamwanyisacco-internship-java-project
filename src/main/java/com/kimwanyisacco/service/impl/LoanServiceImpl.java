package com.kimwanyisacco.service.impl;


import com.kimwanyisacco.dto.request.LoanApplicationRequest;
import com.kimwanyisacco.dto.request.LoanDecisionRequest;
import com.kimwanyisacco.dto.request.LoanRepaymentRequest;
import com.kimwanyisacco.dto.response.LoanResponse;
import com.kimwanyisacco.exception.BusinessRuleViolationException;
import com.kimwanyisacco.exception.ResourceNotFoundException;
import com.kimwanyisacco.model.entity.*;
import com.kimwanyisacco.model.enums.LoanStatus;
import com.kimwanyisacco.repository.*;
import com.kimwanyisacco.service.LoanService;
import com.kimwanyisacco.utils.MoneyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanServiceImpl implements LoanService {

    /** A member cannot apply for a new loan while any of these are open. */
    private static final List<LoanStatus> OPEN_LOAN_STATUSES =
            Arrays.asList(LoanStatus.PENDING, LoanStatus.APPROVED, LoanStatus.ACTIVE);

    private static final int LOAN_TERM_MONTHS = 12;

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final AdminRepository adminRepository;

    @Autowired
    public LoanServiceImpl(LoanRepository loanRepository,
                            LoanRepaymentRepository repaymentRepository,
                            MemberRepository memberRepository,
                            SavingsAccountRepository savingsAccountRepository,
                            AdminRepository adminRepository) {
        this.loanRepository = loanRepository;
        this.repaymentRepository = repaymentRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public LoanResponse applyForLoan(LoanApplicationRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member", request.getMemberId()));

        // Rule: a member may only hold one active loan at a time, and must
        // have fully repaid any previous loan before applying again.
        boolean hasOpenLoan = !loanRepository
                .findByMemberIdAndStatusIn(member.getId(), OPEN_LOAN_STATUSES).isEmpty();
        if (hasOpenLoan) {
            throw new BusinessRuleViolationException(
                    "Member already has an active or pending loan; a new application is not allowed");
        }

        SavingsAccount account = savingsAccountRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SavingsAccount for member", member.getId()));

        // Rule: maximum loan amount is 3x the member's current savings balance.
        BigDecimal maxAllowed = MoneyUtils.maxLoanAmount(account.getBalance());
        if (request.getPrincipalAmount().compareTo(maxAllowed) > 0) {
            throw new BusinessRuleViolationException(String.format(
                    "Requested amount %s exceeds the maximum allowed loan of %s (3x savings balance)",
                    request.getPrincipalAmount(), maxAllowed));
        }

        BigDecimal interest = MoneyUtils.calculateFlatLoanInterest(request.getPrincipalAmount());
        BigDecimal totalRepayable = MoneyUtils.round(request.getPrincipalAmount().add(interest));

        Loan loan = Loan.builder()
                .member(member)
                .principalAmount(request.getPrincipalAmount())
                .interestAmount(interest)
                .totalRepayable(totalRepayable)
                .amountRepaid(BigDecimal.ZERO)
                .status(LoanStatus.PENDING)
                .applicationDate(LocalDate.now())
                .build();

        return toResponse(loanRepository.save(loan));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public LoanResponse decideLoan(Long loanId, LoanDecisionRequest request) {
        Loan loan = findLoanOrThrow(loanId);

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BusinessRuleViolationException("Only a pending loan application can be approved or rejected");
        }

        // Rule: only an admin can approve or reject a loan application.
        // The admin's existence in the Admin table IS the authorization check -
        // a plain Member id would fail to resolve here.
        Admin admin = adminRepository.findById(request.getAdminId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin", request.getAdminId()));

        loan.setApprovedBy(admin);
        loan.setApprovalDate(LocalDate.now());

        if (Boolean.TRUE.equals(request.getApprove())) {
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setDueDate(LocalDate.now().plusMonths(LOAN_TERM_MONTHS));
        } else {
            loan.setStatus(LoanStatus.REJECTED);
        }

        return toResponse(loanRepository.save(loan));
    }

    @Override
    @Transactional
    public LoanResponse repayLoan(LoanRepaymentRequest request) {
        Loan loan = findLoanOrThrow(request.getLoanId());

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Only an active loan can receive repayments");
        }

        BigDecimal newAmountRepaid = MoneyUtils.round(loan.getAmountRepaid().add(request.getAmount()));
        BigDecimal outstanding = loan.getTotalRepayable().subtract(newAmountRepaid);

        loan.setAmountRepaid(newAmountRepaid);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.REPAID); // unlocks eligibility for a future loan
        }
        loanRepository.save(loan);

        LoanRepayment repayment = LoanRepayment.builder()
                .loan(loan)
                .amount(request.getAmount())
                .repaymentDate(LocalDateTime.now())
                .balanceAfter(outstanding.max(BigDecimal.ZERO))
                .build();
        repaymentRepository.save(repayment);

        return toResponse(loan);
    }

    @Override
    public LoanResponse getLoanById(Long loanId) {
        return toResponse(findLoanOrThrow(loanId));
    }

    @Override
    public List<LoanResponse> getLoansByMember(Long memberId) {
        return loanRepository.findByMemberId(memberId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<LoanResponse> getOverdueLoans() {
        // Directly answers "we cannot easily tell who has an overdue loan
        // until we go through every file by hand".
        return loanRepository.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, LocalDate.now()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<LoanResponse> getPendingLoans() {
        return loanRepository.findByStatus(LoanStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Loan findLoanOrThrow(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
    }

    private LoanResponse toResponse(Loan loan) {
        BigDecimal outstanding = loan.getTotalRepayable().subtract(loan.getAmountRepaid());
        return LoanResponse.builder()
                .id(loan.getId())
                .memberId(loan.getMember().getId())
                .principalAmount(loan.getPrincipalAmount())
                .interestAmount(loan.getInterestAmount())
                .totalRepayable(loan.getTotalRepayable())
                .amountRepaid(loan.getAmountRepaid())
                .outstandingBalance(outstanding.max(BigDecimal.ZERO))
                .status(loan.getStatus())
                .applicationDate(loan.getApplicationDate())
                .dueDate(loan.getDueDate())
                .build();
    }
}
