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
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * JSF backing bean for {@code payment.xhtml}.
 *
 * <h3>PesaPal flow</h3>
 * <ol>
 *   <li>Member fills in the form and clicks "Pay with Mobile Money" or "Pay with Card".</li>
 *   <li>{@link #submitMobileMoney()} / {@link #submitCard()} calls the service.</li>
 *   <li>Service contacts PesaPal → returns {@code status=PENDING} + {@code redirectUrl}.</li>
 *   <li>This bean stores the redirect URL and merchant reference on itself (request scope).</li>
 *   <li>The view re-renders showing a banner + auto-opens the PesaPal URL in a new tab.</li>
 *   <li>After the member pays, PesaPal redirects them back to {@code PESAPAL_CALLBACK_URL}.</li>
 *   <li>The frontend (or callback page) polls {@code GET /api/v1/payments/verify/{ref}}.</li>
 * </ol>
 */
@Getter
@Setter
@Component("paymentBean")
@Scope("request")
public class PaymentBean {

    // ── URL params set via <f:viewParam> ──────────────────────────────────────
    private String purpose;    // "SAVINGS_DEPOSIT" or "LOAN_REPAYMENT"
    private Long   targetId;

    // ── Mobile Money fields ───────────────────────────────────────────────────
    private String     phoneNumber;
    private String     mobileProvider = "MTN";
    private BigDecimal mmAmount;

    // ── Card fields ───────────────────────────────────────────────────────────
    private String     cardNumber;
    private String     cardHolderName;
    private Integer    expiryMonth;
    private Integer    expiryYear;
    private String     cvv;
    private String     cardScheme = "VISA";
    private BigDecimal cardAmount;

    // ── Result (populated after successful PesaPal order submission) ──────────
    /** PesaPal redirect URL — non-null means a payment is in PENDING state. */
    private String pendingRedirectUrl;

    /** The merchant reference — used by the polling endpoint. */
    private String pendingMerchantRef;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final PaymentService    paymentService;
    private final CurrentUserContext currentUser;

    @Autowired
    public PaymentBean(PaymentService paymentService, CurrentUserContext currentUser) {
        this.paymentService = paymentService;
        this.currentUser    = currentUser;
    }

    // ── View helpers ──────────────────────────────────────────────────────────

    public List<Integer> getExpiryYears() {
        int current = Year.now().getValue();
        List<Integer> years = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            years.add(current + i);
        }
        return years;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Submits a mobile money payment to PesaPal.
     *
     * <p>On success the service returns {@code status=PENDING} + a {@code redirectUrl}.
     * We store those on this bean and return {@code null} so JSF re-renders the current
     * page. The view shows a banner and auto-opens PesaPal in a new browser tab.
     */
    public String submitMobileMoney() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            MobileMoneyPaymentRequest req = new MobileMoneyPaymentRequest(
                    PaymentPurpose.valueOf(purpose), targetId,
                    phoneNumber, mobileProvider, mmAmount);

            PaymentResponse result = paymentService.payWithMobileMoney(req, currentUser.getMemberId());
            return handleResult(ctx, result);

        } catch (PaymentProcessingException ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ex.getMessage(), null));
            return null;
        } catch (Exception ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Unexpected error: " + ex.getMessage(), null));
            return null;
        }
    }

    /**
     * Submits a card payment to PesaPal.
     *
     * <p>Same flow as mobile money — PCI-DSS card entry happens on PesaPal's
     * hosted page. This server never receives or logs the actual card number/CVV.
     */
    public String submitCard() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            CardPaymentRequest req = new CardPaymentRequest(
                    PaymentPurpose.valueOf(purpose), targetId,
                    cardNumber, cardHolderName,
                    expiryMonth, expiryYear, cvv, cardScheme, cardAmount);

            PaymentResponse result = paymentService.payWithCard(req, currentUser.getMemberId());
            return handleResult(ctx, result);

        } catch (PaymentProcessingException ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ex.getMessage(), null));
            return null;
        } catch (Exception ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Unexpected error: " + ex.getMessage(), null));
            return null;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Handles the service result:
     * <ul>
     *   <li>{@code PENDING} — store redirect URL on this bean, re-render view
     *       (the view auto-opens PesaPal in a new tab via JS)</li>
     *   <li>{@code SUCCESSFUL} — navigate to dashboard with success flash message</li>
     *   <li>Anything else — show error, stay on page</li>
     * </ul>
     */
    private String handleResult(FacesContext ctx, PaymentResponse result) {
        ctx.getExternalContext().getFlash().setKeepMessages(true);

        if (result.getStatus() == PaymentStatus.PENDING && result.getRedirectUrl() != null) {
            // Store for the view to render the banner and the JS auto-open
            this.pendingRedirectUrl  = result.getRedirectUrl();
            this.pendingMerchantRef  = result.getMerchantReference();

            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Payment initiated! Complete it on the PesaPal page that just opened. "
                    + "Reference: " + result.getMerchantReference(), null));
            return null;  // stay on this page; JS will open the PesaPal URL in a new tab
        }

        if (result.getStatus() == PaymentStatus.SUCCESSFUL) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Payment successful. Reference: " + result.getGatewayReference(), null));
            return "dashboard?faces-redirect=true";
        }

        // FAILED / CANCELLED / unexpected
        String reason = result.getFailureReason() != null
                ? result.getFailureReason()
                : (result.getMessage() != null ? result.getMessage() : "Unknown error");
        ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Payment not completed: " + reason, null));
        return null;
    }
}
