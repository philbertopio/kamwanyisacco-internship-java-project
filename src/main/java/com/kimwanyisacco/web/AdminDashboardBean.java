package com.kimwanyisacco.web;


import com.kimwanyisacco.dto.response.LoanResponse;
import com.kimwanyisacco.dto.response.MemberResponse;
import com.kimwanyisacco.dto.response.SavingsAccountResponse;
import com.kimwanyisacco.service.LoanService;
import com.kimwanyisacco.service.MemberService;
import com.kimwanyisacco.service.SavingsService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Component("adminDashboardBean")
@Scope("request")
public class AdminDashboardBean {

    private final MemberService memberService;
    private final SavingsService savingsService;
    private final LoanService loanService;

    private int totalMembers;
    private BigDecimal totalSavingsHeld;
    private int pendingLoansCount;
    private int overdueLoansCount;

    @Autowired
    public AdminDashboardBean(MemberService memberService, SavingsService savingsService, LoanService loanService) {
        this.memberService = memberService;
        this.savingsService = savingsService;
        this.loanService = loanService;
    }

    @PostConstruct
    public void init() {
        List<MemberResponse> members = memberService.getAllMembers();
        this.totalMembers = members.size();

        List<SavingsAccountResponse> accounts = savingsService.getAllAccounts();
        this.totalSavingsHeld = accounts.stream()
                .map(SavingsAccountResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LoanResponse> pending = loanService.getPendingLoans();
        this.pendingLoansCount = pending.size();

        List<LoanResponse> overdue = loanService.getOverdueLoans();
        this.overdueLoansCount = overdue.size();
    }
}
