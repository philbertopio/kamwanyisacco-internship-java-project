package com.kimwanyisacco.model.entity;

import com.kimwanyisacco.model.enums.AccountStatus;
import com.kimwanyisacco.model.entity.Member;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "savings_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "transactions")
public class SavingsAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "min_balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal minBalance = new BigDecimal("20000.00");

    @Column(name = "date_opened", nullable = false)
    private LocalDate dateOpened;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SavingsTransaction> transactions = new ArrayList<>();
}