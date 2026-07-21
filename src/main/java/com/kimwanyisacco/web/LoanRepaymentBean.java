package com.kimwanyisacco.web;


import com.kimwanyisacco.dto.response.LoanResponse;
import com.kimwanyisacco.model.enums.LoanStatus;
import com.kimwanyisacco.service.LoanService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Component("loanRepaymentBean")
@Scope("request")
public class LoanRepaymentBean {

    private final LoanService loanService;
    private final CurrentUserContext currentUser;

    private List<LoanResponse> activeLoans;

    @Autowired
    public LoanRepaymentBean(LoanService loanService, CurrentUserContext currentUser) {
        this.loanService = loanService;
        this.currentUser = currentUser;
    }

    @PostConstruct
    public void init() {
        this.activeLoans = loanService.getLoansByMember(currentUser.getMemberId()).stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .collect(Collectors.toList());
    }
}
