package com.kimwanyisacco.web;

import com.kimwanyisacco.dto.request.DepositRequest;
import com.kimwanyisacco.dto.request.WithdrawalRequest;
import com.kimwanyisacco.dto.response.SavingsAccountResponse;
import com.kimwanyisacco.exception.BusinessRuleViolationException;
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

/**
 * JSF backing bean that bridges the member savings page to the
 * {@link SavingsService} – which is the same service layer consumed
 * by {@link com.kimwanyisacco.controller.SavingsController}.
 *
 * <p>Endpoints exposed by SavingsController and surfaced here:
 * <ul>
 *   <li>GET  /api/v1/savings/member/{memberId}  → {@link #init()} loads the account on page load</li>
 *   <li>GET  /api/v1/savings                    → not exposed to a plain member; admin only</li>
 *   <li>POST /api/v1/savings/deposit            → {@link #deposit()}</li>
 *   <li>POST /api/v1/savings/withdraw           → {@link #withdraw()}</li>
 * </ul>
 *
 * <p>For teller-less self-service (no admin present), the member's own
 * member ID is used as {@code processedByAdminId} so that the service
 * records which principal initiated the transaction, consistent with
 * the comment in {@code SavingsController}.
 */
@Getter
@Setter
@Component("savingsBean")
@Scope("request")
public class SavingsBean {

    // ── page state ────────────────────────────────────────────────────────────
    private SavingsAccountResponse account;

    /** Amount entered by the member for a deposit. */
    private BigDecimal depositAmount;

    /** Amount entered by the member for a withdrawal. */
    private BigDecimal withdrawAmount;

    // ── dependencies ─────────────────────────────────────────────────────────
    private final SavingsService savingsService;
    private final CurrentUserContext currentUser;

    @Autowired
    public SavingsBean(SavingsService savingsService, CurrentUserContext currentUser) {
        this.savingsService = savingsService;
        this.currentUser = currentUser;
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Mirrors: GET /api/v1/savings/member/{memberId}
     */
    @PostConstruct
    public void init() {
        this.account = savingsService.getAccountByMemberId(currentUser.getMemberId());
    }

    // ── actions ──────────────────────────────────────────────────────────────

    /**
     * Mirrors: POST /api/v1/savings/deposit
     * The member's own ID serves as {@code processedByAdminId} for self-service.
     */
    public String deposit() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            DepositRequest req = new DepositRequest(account.getId(), depositAmount);
            this.account = savingsService.deposit(req, currentUser.getMemberId());

            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Deposit of UGX " + depositAmount + " was successful. "
                    + "New balance: UGX " + account.getBalance(), null));
            depositAmount = null;
            return "savings?faces-redirect=true";

        } catch (BusinessRuleViolationException ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        } catch (Exception ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Deposit failed: " + ex.getMessage(), null));
            return null;
        }
    }

    /**
     * Mirrors: POST /api/v1/savings/withdraw
     * The member's own ID serves as {@code processedByAdminId} for self-service.
     */
    public String withdraw() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            WithdrawalRequest req = new WithdrawalRequest(account.getId(), withdrawAmount);
            this.account = savingsService.withdraw(req, currentUser.getMemberId());

            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Withdrawal of UGX " + withdrawAmount + " was successful. "
                    + "New balance: UGX " + account.getBalance(), null));
            withdrawAmount = null;
            return "savings?faces-redirect=true";

        } catch (BusinessRuleViolationException ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        } catch (Exception ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Withdrawal failed: " + ex.getMessage(), null));
            return null;
        }
    }

    // ── helpers used by the view ──────────────────────────────────────────────

    /**
     * Available balance that can be withdrawn (balance minus minimum required).
     */
    public BigDecimal getAvailableBalance() {
        if (account == null) return BigDecimal.ZERO;
        BigDecimal available = account.getBalance().subtract(account.getMinBalance());
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    /**
     * True when the account is active (i.e. transactions are allowed).
     */
    public boolean isAccountActive() {
        return account != null && "ACTIVE".equalsIgnoreCase(account.getStatus().name());
    }

    /**
     * True when available balance is zero or negative.
     * <p>
     * JSF EL cannot resolve static fields (e.g. {@code java.math.BigDecimal.ZERO})
     * so this comparison must live in Java, not in an EL expression.
     */
    public boolean isNoFundsAvailable() {
        return getAvailableBalance().compareTo(BigDecimal.ZERO) <= 0;
    }
}
