package com.kimwanyisacco.model.entity;

import com.kimwanyisacco.model.enums.LoanStatus;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "repayments")
public class Loan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private SaccoMember member;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "total_repayable", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalRepayable;

    @Column(name = "amount_repaid", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal amountRepaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    // Nullable: no approver until an admin acts on the application
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Admin approvedBy;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @OneToMany(mappedBy = "loan", fetch = FetchType.LAZY)
    @Builder.Default
    private List<LoanRepayment> repayments = new ArrayList<>();
}

