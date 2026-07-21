package com.kimwanyisacco.web;


import com.kimwanyisacco.dto.request.CardPaymentRequest;
import com.kimwanyisacco.dto.request.MobileMoneyPaymentRequest;
import com.kimwanyisacco.dto.response.PaymentResponse;
import com.kimwanyisacco.exception.PaymentProcessingException;
import com.kimwanyisacco.model.enums.PaymentPurpose;
import com.kimwanyisacco.model.enums.PaymentStatus;
import com.kimwanyisacco.service.PaymentService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component("paymentBean")
@Scope("request")
public class PaymentBean {

    // Set via <f:viewParam> from the linking page (dashboard / repayLoan)
    private String purpose;   // "SAVINGS_DEPOSIT" or "LOAN_REPAYMENT"
    private Long targetId;

    private String selectedMethod = "MOBILE_MONEY"; // default active tab

    // Mobile Money fields
    private String phoneNumber;
    private String mobileProvider = "MTN";
    private BigDecimal mmAmount;

    // Card fields
    private String cardNumber;
    private String cardHolderName;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cvv;
    private String cardScheme = "VISA";
    private BigDecimal cardAmount;

    private PaymentResponse lastResult;

    private final PaymentService paymentService;
    private final CurrentUserContext currentUser;

    @Autowired
    public PaymentBean(PaymentService paymentService, CurrentUserContext currentUser) {
        this.paymentService = paymentService;
        this.currentUser = currentUser;
    }

    public List<Integer> getExpiryYears() {
        int current = Year.now().getValue();
        List<Integer> years = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            years.add(current + i);
        }
        return years;
    }

    public String submitMobileMoney() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            MobileMoneyPaymentRequest request = new MobileMoneyPaymentRequest(
                    PaymentPurpose.valueOf(purpose), targetId, phoneNumber, mobileProvider, mmAmount);
            lastResult = paymentService.payWithMobileMoney(request, currentUser.getMemberId());
            return handleResult(context);
        } catch (PaymentProcessingException ex) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        }
    }

    public String submitCard() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            CardPaymentRequest request = new CardPaymentRequest(
                    PaymentPurpose.valueOf(purpose), targetId, cardNumber, cardHolderName,
                    expiryMonth, expiryYear, cvv, cardScheme, cardAmount);
            lastResult = paymentService.payWithCard(request, currentUser.getMemberId());
            return handleResult(context);
        } catch (PaymentProcessingException ex) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        }
    }

    private String handleResult(FacesContext context) {
        context.getExternalContext().getFlash().setKeepMessages(true);
        if (lastResult.getStatus() == PaymentStatus.SUCCESSFUL) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Payment successful. Reference: " + lastResult.getGatewayReference(), null));
            return "dashboard?faces-redirect=true";
        }
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Payment did not go through: " + lastResult.getFailureReason(), null));
        return null;
    }
}
