package com.kimwanyisacco.web;


import com.kimwanyisacco.dto.request.LoanApplicationRequest;
import com.kimwanyisacco.dto.response.SavingsAccountResponse;
import com.kimwanyisacco.exception.BusinessRuleViolationException;
import com.kimwanyisacco.service.LoanService;
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

@Getter
@Setter
@Component("loanApplicationBean")
@Scope("request")
public class LoanApplicationBean {

    private BigDecimal principalAmount;
    private BigDecimal maxEligibleAmount;

    private final LoanService loanService;
    private final SavingsService savingsService;
    private final CurrentUserContext currentUser;

    @Autowired
    public LoanApplicationBean(LoanService loanService, SavingsService savingsService, CurrentUserContext currentUser) {
        this.loanService = loanService;
        this.savingsService = savingsService;
        this.currentUser = currentUser;
    }

    @PostConstruct
    public void init() {
        SavingsAccountResponse account = savingsService.getAccountByMemberId(currentUser.getMemberId());
        this.maxEligibleAmount = account.getBalance().multiply(BigDecimal.valueOf(3));
    }

    public String apply() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            LoanApplicationRequest request = new LoanApplicationRequest(currentUser.getMemberId(), principalAmount);
            loanService.applyForLoan(request);

            context.getExternalContext().getFlash().setKeepMessages(true);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Loan application submitted. An admin will review it shortly.", null));
            return "dashboard?faces-redirect=true";

        } catch (BusinessRuleViolationException ex) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        }
    }
}
