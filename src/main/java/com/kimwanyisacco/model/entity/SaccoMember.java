package com.kimwanyisacco.model.entity;

import com.kimwanyisacco.model.enums.AccountStatus;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_members_national_id", columnNames = "national_id"),
        @UniqueConstraint(name = "uk_members_membership_number", columnNames = "membership_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"savingsAccount", "loans"})
public class SaccoMember extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "national_id", nullable = false, length = 30)
    private String nationalId;

    @Column(name = "membership_number", nullable = false, length = 30)
    private String membershipNumber;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "date_joined", nullable = false)
    private LocalDate dateJoined;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SavingsAccount savingsAccount;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Loan> loans = new ArrayList<>();
}