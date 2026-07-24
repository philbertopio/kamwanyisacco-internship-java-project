package com.kimwanyisacco.web;

import com.kimwanyisacco.dto.request.DepositRequest;
import com.kimwanyisacco.dto.request.WithdrawalRequest;
import com.kimwanyisacco.dto.response.LoanResponse;
import com.kimwanyisacco.dto.response.MemberResponse;
import com.kimwanyisacco.dto.response.SavingsAccountResponse;
import com.kimwanyisacco.exception.BusinessRuleViolationException;
import com.kimwanyisacco.model.enums.LoanStatus;
import com.kimwanyisacco.service.LoanService;
import com.kimwanyisacco.service.MemberService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Backing bean for the enhanced admin dashboard.
 *
 * <p>Computes KPIs, risk alerts, member roster, and pre-serialises chart data
 * as JSON strings so the Facelets view can embed them directly into Chart.js
 * dataset configurations without any extra AJAX endpoint.
 */
@Getter
@Setter
@Component("adminDashboardBean")
@Scope("request")
public class AdminDashboardBean {

    // ── services ─────────────────────────────────────────────────────────────
    private final MemberService memberService;
    private final SavingsService savingsService;
    private final LoanService loanService;
    private final CurrentUserContext currentUser;

    // ── KPI: members ──────────────────────────────────────────────────────────
    private int totalMembers;
    private int activeMembers;
    private int inactiveMembers;
    private int newMembersThisMonth;

    // ── KPI: savings ─────────────────────────────────────────────────────────
    private BigDecimal totalSavingsHeld;
    private BigDecimal averageSavingsPerMember;

    // ── KPI: loans ───────────────────────────────────────────────────────────
    private int pendingLoansCount;
    private int activeLoansCount;
    private int overdueLoansCount;
    private BigDecimal totalLoanPortfolio;
    private BigDecimal totalOverdueAmount;
    private int repaymentRatePct;          // 0–100, for the progress bar widget

    // ── lists used by UI ─────────────────────────────────────────────────────
    private List<MemberResponse> allMembers;
    private List<SavingsAccountResponse> allAccounts;
    private List<LoanResponse> overdueLoans;
    private List<LoanResponse> atRiskLoans;   // due within 7 days

    // ── Chart.js JSON payloads ────────────────────────────────────────────────
    /** Donut: loan count per status. */
    private String loanStatusChartJson;
    /** Bar: count of members in each savings-balance bracket. */
    private String savingsBracketChartJson;

    // ── teller action fields (deposit / withdraw on behalf of member) ─────────
    /** Membership number entered by the teller, e.g. KIM-3B1B0E03. */
    private String tellerMemberId;
    private BigDecimal tellerAmount;

    @Autowired
    public AdminDashboardBean(MemberService memberService,
                              SavingsService savingsService,
                              LoanService loanService,
                              CurrentUserContext currentUser) {
        this.memberService = memberService;
        this.savingsService = savingsService;
        this.loanService = loanService;
        this.currentUser = currentUser;
    }

    @PostConstruct
    public void init() {
        // ── members ──────────────────────────────────────────────────────────
        allMembers = memberService.getAllMembers();
        totalMembers = allMembers.size();
        activeMembers = (int) allMembers.stream()
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus().name()))
                .count();
        inactiveMembers = totalMembers - activeMembers;
        newMembersThisMonth = (int) allMembers.stream()
                .filter(m -> m.getDateJoined() != null
                        && m.getDateJoined().getMonthValue() == LocalDate.now().getMonthValue()
                        && m.getDateJoined().getYear() == LocalDate.now().getYear())
                .count();

        // ── savings ──────────────────────────────────────────────────────────
        allAccounts = savingsService.getAllAccounts();
        totalSavingsHeld = allAccounts.stream()
                .map(SavingsAccountResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        averageSavingsPerMember = totalMembers > 0
                ? totalSavingsHeld.divide(BigDecimal.valueOf(totalMembers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ── loans ─────────────────────────────────────────────────────────────
        List<LoanResponse> allLoans = loanService.getAllLoans();
        List<LoanResponse> pending = loanService.getPendingLoans();
        List<LoanResponse> overdue  = loanService.getOverdueLoans();

        pendingLoansCount = pending.size();
        overdueLoans = overdue;
        overdueLoansCount = overdue.size();

        activeLoansCount = (int) allLoans.stream()
                .filter(l -> LoanStatus.ACTIVE.name().equals(l.getStatus().name()))
                .count();

        totalLoanPortfolio = allLoans.stream()
                .filter(l -> LoanStatus.ACTIVE.name().equals(l.getStatus().name()))
                .map(LoanResponse::getTotalRepayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalOverdueAmount = overdue.stream()
                .map(LoanResponse::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Repayment collection rate: amountRepaid / totalRepayable across ALL loans
        BigDecimal totalRepayable = allLoans.stream()
                .map(LoanResponse::getTotalRepayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRepaid = allLoans.stream()
                .map(LoanResponse::getAmountRepaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        repaymentRatePct = totalRepayable.compareTo(BigDecimal.ZERO) > 0
                ? totalRepaid.multiply(BigDecimal.valueOf(100))
                             .divide(totalRepayable, 0, RoundingMode.HALF_UP)
                             .intValue()
                : 0;

        // At-risk loans: active, not yet overdue, but due within 7 days
        LocalDate soonCutoff = LocalDate.now().plusDays(7);
        atRiskLoans = allLoans.stream()
                .filter(l -> LoanStatus.ACTIVE.name().equals(l.getStatus().name())
                        && l.getDueDate() != null
                        && !l.getDueDate().isBefore(LocalDate.now())
                        && !l.getDueDate().isAfter(soonCutoff))
                .collect(Collectors.toList());

        // ── chart JSON ────────────────────────────────────────────────────────
        buildLoanStatusChartJson(allLoans);
        buildSavingsBracketChartJson();
    }

    // ── teller actions ────────────────────────────────────────────────────────

    /** Teller-initiated deposit — uses the admin's own ID as processedByAdminId. */
    public String tellerDeposit() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            DepositRequest req = new DepositRequest(resolveTellerAccountId(), tellerAmount);
            savingsService.deposit(req, currentUser.getAdminId());
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Teller deposit of UGX " + tellerAmount + " processed successfully.", null));
            return "dashboard?faces-redirect=true";
        } catch (BusinessRuleViolationException ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        } catch (Exception ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Deposit failed: " + ex.getMessage(), null));
            return null;
        }
    }

    /** Teller-initiated withdrawal — uses the admin's own ID as processedByAdminId. */
    public String tellerWithdraw() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            WithdrawalRequest req = new WithdrawalRequest(resolveTellerAccountId(), tellerAmount);
            savingsService.withdraw(req, currentUser.getAdminId());
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Teller withdrawal of UGX " + tellerAmount + " processed successfully.", null));
            return "dashboard?faces-redirect=true";
        } catch (BusinessRuleViolationException ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        } catch (Exception ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Withdrawal failed: " + ex.getMessage(), null));
            return null;
        }
    }

    /**
     * Resolves the human-facing membership number to the internal savings-account ID.
     * Transaction services continue to receive account IDs, preserving their existing
     * balance, minimum-balance, authorization, and audit rules.
     */
    private Long resolveTellerAccountId() {
        String membershipNumber = tellerMemberId == null ? "" : tellerMemberId.trim();
        MemberResponse member = allMembers.stream()
                .filter(candidate -> candidate.getMembershipNumber() != null
                        && candidate.getMembershipNumber().equalsIgnoreCase(membershipNumber))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No member was found with ID " + membershipNumber + "."));

        return allAccounts.stream()
                .filter(account -> member.getId().equals(account.getMemberId()))
                .map(SavingsAccountResponse::getId)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Member " + membershipNumber + " does not have a savings account."));
    }

    /** Deactivates a member account. */
    public String deactivateMember(Long memberId) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            memberService.deactivateMember(memberId);
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Member #" + memberId + " deactivated.", null));
        } catch (Exception ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
        }
        return "dashboard?faces-redirect=true";
    }

    /** Reactivates a member account. */
    public String reactivateMember(Long memberId) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            memberService.reactivateMember(memberId);
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Member #" + memberId + " reactivated.", null));
        } catch (Exception ex) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
        }
        return "dashboard?faces-redirect=true";
    }

    // ── chart builders ────────────────────────────────────────────────────────

    private void buildLoanStatusChartJson(List<LoanResponse> loans) {
        Map<String, Long> counts = loans.stream()
                .collect(Collectors.groupingBy(l -> l.getStatus().name(), Collectors.counting()));

        long pending  = counts.getOrDefault("PENDING",  0L);
        long active   = counts.getOrDefault("ACTIVE",   0L);
        long repaid   = counts.getOrDefault("REPAID",   0L);
        long rejected = counts.getOrDefault("REJECTED", 0L);
        long overdue  = overdueLoansCount;

        loanStatusChartJson = String.format(
            "{\"labels\":[\"Pending\",\"Active\",\"Repaid\",\"Rejected\",\"Overdue\"]," +
            "\"datasets\":[{\"data\":[%d,%d,%d,%d,%d]," +
            "\"backgroundColor\":[\"#FEF9C3\",\"#DCFCE7\",\"#E6F4EC\",\"#FEF2F2\",\"#DC2626\"]," +
            "\"borderColor\":[\"#A16207\",\"#16A34A\",\"#0F5132\",\"#DC2626\",\"#991B1B\"]," +
            "\"borderWidth\":2}]}",
            pending, active, repaid, rejected, overdue
        );
    }

    private void buildSavingsBracketChartJson() {
        long b0   = allAccounts.stream().filter(a -> a.getBalance().compareTo(BigDecimal.valueOf(50_000)) < 0).count();
        long b50  = allAccounts.stream().filter(a -> {
            BigDecimal b = a.getBalance();
            return b.compareTo(BigDecimal.valueOf(50_000)) >= 0 && b.compareTo(BigDecimal.valueOf(200_000)) < 0;
        }).count();
        long b200 = allAccounts.stream().filter(a -> {
            BigDecimal b = a.getBalance();
            return b.compareTo(BigDecimal.valueOf(200_000)) >= 0 && b.compareTo(BigDecimal.valueOf(500_000)) < 0;
        }).count();
        long b500 = allAccounts.stream().filter(a -> a.getBalance().compareTo(BigDecimal.valueOf(500_000)) >= 0).count();

        savingsBracketChartJson = String.format(
            "{\"labels\":[\"0–50k\",\"50k–200k\",\"200k–500k\",\"500k+\"]," +
            "\"datasets\":[{\"label\":\"Members\"," +
            "\"data\":[%d,%d,%d,%d]," +
            "\"backgroundColor\":[\"#E6F4EC\",\"#BBF7D0\",\"#4ADE80\",\"#16A34A\"]," +
            "\"borderRadius\":6,\"borderSkipped\":false}]}",
            b0, b50, b200, b500
        );
    }

    // ── view helpers ──────────────────────────────────────────────────────────

    /** Looks up the savings balance for a given member — used in the member table. */
    public BigDecimal getSavingsBalanceForMember(Long memberId) {
        return allAccounts.stream()
                .filter(a -> memberId.equals(a.getMemberId()))
                .map(SavingsAccountResponse::getBalance)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    /** Returns the number of days a loan is overdue (positive = overdue). */
    public long getDaysOverdue(LoanResponse loan) {
        if (loan.getDueDate() == null) return 0;
        long days = LocalDate.now().toEpochDay() - loan.getDueDate().toEpochDay();
        return Math.max(0, days);
    }
}
