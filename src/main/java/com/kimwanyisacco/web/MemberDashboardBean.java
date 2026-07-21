package com.kimwanyisacco.web;




import com.kimwanyisacco.dto.response.LoanResponse;
import com.kimwanyisacco.dto.response.MemberResponse;
import com.kimwanyisacco.dto.response.SavingsAccountResponse;
import com.kimwanyisacco.service.LoanService;
import com.kimwanyisacco.service.MemberService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Getter
@Component("memberDashboardBean")
@Scope("request")
public class MemberDashboardBean {

    private final MemberService memberService;
    private final SavingsService savingsService;
    private final LoanService loanService;
    private final CurrentUserContext currentUser;

    private MemberResponse member;
    private SavingsAccountResponse savingsAccount;
    private List<LoanResponse> loans;

    @Autowired
    public MemberDashboardBean(MemberService memberService, SavingsService savingsService,
                               LoanService loanService, CurrentUserContext currentUser) {
        this.memberService = memberService;
        this.savingsService = savingsService;
        this.loanService = loanService;
        this.currentUser = currentUser;
    }

    @PostConstruct
    public void init() {
        Long memberId = currentUser.getMemberId();
        this.member = memberService.getMemberById(memberId);
        this.savingsAccount = savingsService.getAccountByMemberId(memberId);
        this.loans = loanService.getLoansByMember(memberId);
    }

    public boolean isHasActiveLoan() {
        return loans != null && loans.stream().anyMatch(l -> "ACTIVE".equals(l.getStatus().name()));
    }
}

