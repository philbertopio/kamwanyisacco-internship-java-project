package com.kimwanyisacco.web;




import com.kimwanyisacco.dto.response.LoanResponse;
import com.kimwanyisacco.dto.response.MemberResponse;
import com.kimwanyisacco.dto.response.SavingsAccountResponse;
import com.kimwanyisacco.service.LoanService;
import com.kimwanyisacco.service.MemberService;
import com.kimwanyisacco.service.MemberTransactionApprovalService;
import com.kimwanyisacco.service.SavingsService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Component("memberDashboardBean")
@Scope("request")
public class MemberDashboardBean {

    private final MemberService memberService;
    private final SavingsService savingsService;
    private final LoanService loanService;
    private final MemberTransactionApprovalService transactionApprovalService;
    private final CurrentUserContext currentUser;

    private MemberResponse member;
    private SavingsAccountResponse savingsAccount;
    private List<LoanResponse> loans;
    private BigDecimal depositAmount;
    private BigDecimal withdrawalAmount;
    private BigDecimal repaymentAmount;

    @Autowired
    public MemberDashboardBean(MemberService memberService, SavingsService savingsService,
                               LoanService loanService,
                               MemberTransactionApprovalService transactionApprovalService,
                               CurrentUserContext currentUser) {
        this.memberService = memberService;
        this.savingsService = savingsService;
        this.loanService = loanService;
        this.transactionApprovalService = transactionApprovalService;
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

    public String requestDeposit() {
        return submitRequest(() -> transactionApprovalService.requestSavingsDeposit(
                currentUser.getMemberId(), savingsAccount.getId(), depositAmount), "Deposit");
    }

    public String requestWithdrawal() {
        return submitRequest(() -> transactionApprovalService.requestSavingsWithdrawal(
                currentUser.getMemberId(), savingsAccount.getId(), withdrawalAmount), "Withdrawal");
    }

    public String requestLoanRepayment(Long loanId) {
        return submitRequest(() -> transactionApprovalService.requestLoanRepayment(
                currentUser.getMemberId(), loanId, repaymentAmount), "Loan repayment");
    }

    private String submitRequest(Runnable request, String transactionName) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            request.run();
            context.getExternalContext().getFlash().setKeepMessages(true);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    transactionName + " request submitted for admin approval. Your balance will update after approval.", null));
            return "dashboard?faces-redirect=true";
        } catch (Exception ex) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    transactionName + " request failed: " + ex.getMessage(), null));
            return null;
        }
    }
}

