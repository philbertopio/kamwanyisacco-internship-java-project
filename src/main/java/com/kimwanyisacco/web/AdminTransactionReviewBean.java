package com.kimwanyisacco.web;

import com.kimwanyisacco.dto.response.MemberTransactionApprovalResponse;
import com.kimwanyisacco.service.MemberTransactionApprovalService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.List;

@Getter
@Component("adminTransactionReviewBean")
@Scope("request")
public class AdminTransactionReviewBean {

    private final MemberTransactionApprovalService transactionApprovalService;
    private final CurrentUserContext currentUser;
    private List<MemberTransactionApprovalResponse> pendingRequests;

    @Autowired
    public AdminTransactionReviewBean(MemberTransactionApprovalService transactionApprovalService,
                                      CurrentUserContext currentUser) {
        this.transactionApprovalService = transactionApprovalService;
        this.currentUser = currentUser;
    }

    @PostConstruct
    public void init() {
        pendingRequests = transactionApprovalService.getPendingRequests();
    }

    public void approve(Long requestId) {
        decide(requestId, true);
    }

    public void reject(Long requestId) {
        decide(requestId, false);
    }

    private void decide(Long requestId, boolean approve) {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            if (approve) {
                transactionApprovalService.approve(requestId, currentUser.getAdminId());
            } else {
                transactionApprovalService.reject(requestId, currentUser.getAdminId());
            }
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Transaction request " + (approve ? "approved" : "rejected") + ".", null));
            init();
        } catch (Exception ex) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Transaction request could not be processed: " + ex.getMessage(), null));
        }
    }
}
