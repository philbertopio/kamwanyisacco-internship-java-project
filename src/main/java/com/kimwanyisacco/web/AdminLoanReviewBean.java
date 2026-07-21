package com.kimwanyisacco.web;

import com.kimwanyisacco.dto.request.LoanDecisionRequest;
import com.kimwanyisacco.dto.response.LoanResponse;
import com.kimwanyisacco.service.LoanService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.List;

@Getter
@Component("adminLoanReviewBean")
@Scope("request")
public class AdminLoanReviewBean {

    private final LoanService loanService;
    private final CurrentUserContext currentUser;

    private List<LoanResponse> pendingLoans;

    @Autowired
    public AdminLoanReviewBean(LoanService loanService, CurrentUserContext currentUser) {
        this.loanService = loanService;
        this.currentUser = currentUser;
    }

    @PostConstruct
    public void init() {
        this.pendingLoans = loanService.getPendingLoans();
    }

    public void approve(Long loanId) {
        decide(loanId, true);
    }

    public void reject(Long loanId) {
        decide(loanId, false);
    }

    private void decide(Long loanId, boolean approve) {
        FacesContext context = FacesContext.getCurrentInstance();
        LoanDecisionRequest request = new LoanDecisionRequest(currentUser.getAdminId(), approve);
        loanService.decideLoan(loanId, request);
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Loan " + (approve ? "approved" : "rejected") + ".", null));
        init(); // refresh the pending list after the decision
    }
}
