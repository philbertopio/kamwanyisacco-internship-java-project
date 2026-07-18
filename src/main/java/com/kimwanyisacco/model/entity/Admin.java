package com.kimwanyisacco.model.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "admins", uniqueConstraints = {
        @UniqueConstraint(name = "uk_admins_staff_id", columnNames = "staff_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Admin extends BaseEntity{

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "staff_id", nullable = false, length = 30)
    private String staffId;

    @Column(name = "department", length = 100)
    private String department;
}
