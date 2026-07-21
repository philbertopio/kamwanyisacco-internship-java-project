package com.kimwanyisacco.service.impl;

import com.kimwanyisacco.dto.request.DepositRequest;
import com.kimwanyisacco.dto.request.WithdrawalRequest;
import com.kimwanyisacco.dto.response.SavingsAccountResponse;
import com.kimwanyisacco.exception.InsufficientBalanceException;
import com.kimwanyisacco.exception.ResourceNotFoundException;
import com.kimwanyisacco.model.entity.Admin;
import com.kimwanyisacco.model.entity.SavingsAccount;
import com.kimwanyisacco.model.entity.SavingsTransaction;
import com.kimwanyisacco.model.enums.TransactionType;
import com.kimwanyisacco.repository.AdminRepository;
import com.kimwanyisacco.repository.SavingsAccountRepository;
import com.kimwanyisacco.repository.SavingsTransactionRepository;
import com.kimwanyisacco.service.SavingsService;
import com.kimwanyisacco.utils.MoneyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SavingsServiceImpl implements SavingsService {

    private final SavingsAccountRepository accountRepository;
    private final SavingsTransactionRepository transactionRepository;
    private final AdminRepository adminRepository;

    @Autowired
    public SavingsServiceImpl(SavingsAccountRepository accountRepository,
                               SavingsTransactionRepository transactionRepository,
                               AdminRepository adminRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public SavingsAccountResponse getAccountByMemberId(Long memberId) {
        return toResponse(findAccountByMemberOrThrow(memberId));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public SavingsAccountResponse deposit(DepositRequest request, Long processedByAdminId) {
        SavingsAccount account = findAccountOrThrow(request.getAccountId());

        BigDecimal newBalance = MoneyUtils.round(account.getBalance().add(request.getAmount()));
        account.setBalance(newBalance);
        accountRepository.save(account);

        logTransaction(account, TransactionType.DEPOSIT, request.getAmount(), newBalance, processedByAdminId);

        return toResponse(account);
    }

    @Override
    @Transactional
    public SavingsAccountResponse creditFromPayment(Long accountId, BigDecimal amount, String paymentReference) {
        SavingsAccount account = findAccountOrThrow(accountId);

        BigDecimal newBalance = MoneyUtils.round(account.getBalance().add(amount));
        account.setBalance(newBalance);
        accountRepository.save(account);

        // processedByAdminId is null here - this was a self-service Mobile
        // Money/Card payment, not a teller-handled transaction. The
        // paymentReference lives on the Payment record for reconciliation.
        logTransaction(account, TransactionType.DEPOSIT, amount, newBalance, null);

        return toResponse(account);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public SavingsAccountResponse withdraw(WithdrawalRequest request, Long processedByAdminId) {
        SavingsAccount account = findAccountOrThrow(request.getAccountId());

        BigDecimal balanceAfterWithdrawal = account.getBalance().subtract(request.getAmount());

        // Two rules enforced together here:
        // 1) cannot withdraw more than available balance
        // 2) minimum savings balance of UGX 20,000 must always be maintained
        // This directly fixes "a member withdraws more than they have because
        // the cashier misread the balance" - the system now blocks it outright.
        if (balanceAfterWithdrawal.compareTo(account.getMinBalance()) < 0) {
            throw new InsufficientBalanceException(String.format(
                    "Withdrawal declined: balance would fall to %s, below the required minimum of %s",
                    balanceAfterWithdrawal, account.getMinBalance()));
        }

        BigDecimal newBalance = MoneyUtils.round(balanceAfterWithdrawal);
        account.setBalance(newBalance);
        accountRepository.save(account);

        logTransaction(account, TransactionType.WITHDRAWAL, request.getAmount(), newBalance, processedByAdminId);

        return toResponse(account);
    }

    @Override
    @Transactional
    public void applyMonthlyInterest(Long accountId) {
        SavingsAccount account = findAccountOrThrow(accountId);
        BigDecimal interest = MoneyUtils.calculateMonthlySavingsInterest(account.getBalance());

        BigDecimal newBalance = MoneyUtils.round(account.getBalance().add(interest));
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Interest postings are system-generated, so processedByAdminId is null.
        logTransaction(account, TransactionType.DEPOSIT, interest, newBalance, null);
    }

    @Override
    public List<SavingsAccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void logTransaction(SavingsAccount account, TransactionType type, BigDecimal amount,
                                 BigDecimal balanceAfter, Long processedByAdminId) {
        Admin processedBy = null;
        if (processedByAdminId != null) {
            processedBy = adminRepository.findById(processedByAdminId)
                    .orElseThrow(() -> new ResourceNotFoundException("Admin", processedByAdminId));
        }

        SavingsTransaction transaction = SavingsTransaction.builder()
                .account(account)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .transactionDate(LocalDateTime.now())
                .processedBy(processedBy)
                .build();
        transactionRepository.save(transaction);
    }

    private SavingsAccount findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("SavingsAccount", accountId));
    }

    private SavingsAccount findAccountByMemberOrThrow(Long memberId) {
        return accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("SavingsAccount for member", memberId));
    }

    private SavingsAccountResponse toResponse(SavingsAccount account) {
        return SavingsAccountResponse.builder()
                .id(account.getId())
                .memberId(account.getMember().getId())
                .balance(account.getBalance())
                .minBalance(account.getMinBalance())
                .status(account.getStatus())
                .build();
    }
}
