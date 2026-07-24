package com.kimwanyisacco.service.impl;

import com.kimwanyisacco.dto.request.DepositRequest;
import com.kimwanyisacco.dto.request.LoanRepaymentRequest;
import com.kimwanyisacco.dto.request.WithdrawalRequest;
import com.kimwanyisacco.dto.response.MemberTransactionApprovalResponse;
import com.kimwanyisacco.exception.BusinessRuleViolationException;
import com.kimwanyisacco.exception.ResourceNotFoundException;
import com.kimwanyisacco.model.entity.Admin;
import com.kimwanyisacco.model.entity.Loan;
import com.kimwanyisacco.model.entity.Member;
import com.kimwanyisacco.model.entity.MemberTransactionApproval;
import com.kimwanyisacco.model.entity.SavingsAccount;
import com.kimwanyisacco.model.enums.LoanStatus;
import com.kimwanyisacco.model.enums.MemberTransactionApprovalStatus;
import com.kimwanyisacco.model.enums.MemberTransactionType;
import com.kimwanyisacco.repository.AdminRepository;
import com.kimwanyisacco.repository.LoanRepository;
import com.kimwanyisacco.repository.MemberRepository;
import com.kimwanyisacco.repository.MemberTransactionApprovalRepository;
import com.kimwanyisacco.repository.SavingsAccountRepository;
import com.kimwanyisacco.service.LoanService;
import com.kimwanyisacco.service.MemberTransactionApprovalService;
import com.kimwanyisacco.service.SavingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemberTransactionApprovalServiceImpl implements MemberTransactionApprovalService {

    private final MemberTransactionApprovalRepository approvalRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    private final AdminRepository adminRepository;
    private final SavingsService savingsService;
    private final LoanService loanService;

    @Autowired
    public MemberTransactionApprovalServiceImpl(MemberTransactionApprovalRepository approvalRepository,
                                                MemberRepository memberRepository,
                                                SavingsAccountRepository savingsAccountRepository,
                                                LoanRepository loanRepository,
                                                AdminRepository adminRepository,
                                                SavingsService savingsService,
                                                LoanService loanService) {
        this.approvalRepository = approvalRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.loanRepository = loanRepository;
        this.adminRepository = adminRepository;
        this.savingsService = savingsService;
        this.loanService = loanService;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('MEMBER')")
    public void requestSavingsDeposit(Long memberId, Long savingsAccountId, BigDecimal amount) {
        createSavingsRequest(memberId, savingsAccountId, amount, MemberTransactionType.SAVINGS_DEPOSIT);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('MEMBER')")
    public void requestSavingsWithdrawal(Long memberId, Long savingsAccountId, BigDecimal amount) {
        createSavingsRequest(memberId, savingsAccountId, amount, MemberTransactionType.SAVINGS_WITHDRAWAL);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('MEMBER')")
    public void requestLoanRepayment(Long memberId, Long loanId, BigDecimal amount) {
        validateAmount(amount);
        Member member = findMember(memberId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        if (!member.getId().equals(loan.getMember().getId())) {
            throw new BusinessRuleViolationException("You may only repay your own loan.");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Only an active loan can receive repayments.");
        }
        saveRequest(member, null, loan, amount, MemberTransactionType.LOAN_REPAYMENT);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<MemberTransactionApprovalResponse> getPendingRequests() {
        return approvalRepository.findPendingWithTargets(MemberTransactionApprovalStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void approve(Long requestId, Long adminId) {
        MemberTransactionApproval request = findPendingRequest(requestId);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminId));

        switch (request.getTransactionType()) {
            case SAVINGS_DEPOSIT -> savingsService.deposit(
                    new DepositRequest(request.getSavingsAccount().getId(), request.getAmount()), adminId);
            case SAVINGS_WITHDRAWAL -> savingsService.withdraw(
                    new WithdrawalRequest(request.getSavingsAccount().getId(), request.getAmount()), adminId);
            case LOAN_REPAYMENT -> loanService.repayLoan(
                    new LoanRepaymentRequest(request.getLoan().getId(), request.getAmount()));
            default -> throw new BusinessRuleViolationException("Unsupported transaction request type.");
        }

        request.setStatus(MemberTransactionApprovalStatus.APPROVED);
        request.setProcessedBy(admin);
        request.setProcessedAt(LocalDateTime.now());
        approvalRepository.save(request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void reject(Long requestId, Long adminId) {
        MemberTransactionApproval request = findPendingRequest(requestId);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminId));
        request.setStatus(MemberTransactionApprovalStatus.REJECTED);
        request.setProcessedBy(admin);
        request.setProcessedAt(LocalDateTime.now());
        approvalRepository.save(request);
    }

    private void createSavingsRequest(Long memberId, Long savingsAccountId, BigDecimal amount,
                                      MemberTransactionType transactionType) {
        validateAmount(amount);
        Member member = findMember(memberId);
        SavingsAccount account = savingsAccountRepository.findById(savingsAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("SavingsAccount", savingsAccountId));
        if (!member.getId().equals(account.getMember().getId())) {
            throw new BusinessRuleViolationException("You may only request a transaction for your own savings account.");
        }
        saveRequest(member, account, null, amount, transactionType);
    }

    private void saveRequest(Member member, SavingsAccount account, Loan loan, BigDecimal amount,
                             MemberTransactionType transactionType) {
        approvalRepository.save(MemberTransactionApproval.builder()
                .member(member)
                .savingsAccount(account)
                .loan(loan)
                .amount(amount)
                .transactionType(transactionType)
                .status(MemberTransactionApprovalStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build());
    }

    private MemberTransactionApproval findPendingRequest(Long requestId) {
        MemberTransactionApproval request = approvalRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Member transaction request", requestId));
        if (request.getStatus() != MemberTransactionApprovalStatus.PENDING) {
            throw new BusinessRuleViolationException("This transaction request has already been processed.");
        }
        return request;
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Amount must be greater than zero.");
        }
    }

    private MemberTransactionApprovalResponse toResponse(MemberTransactionApproval request) {
        return MemberTransactionApprovalResponse.builder()
                .id(request.getId())
                .memberId(request.getMember().getId())
                .membershipNumber(request.getMember().getMembershipNumber())
                .savingsAccountId(request.getSavingsAccount() == null ? null : request.getSavingsAccount().getId())
                .loanId(request.getLoan() == null ? null : request.getLoan().getId())
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .build();
    }
}
