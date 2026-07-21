package com.kimwanyisacco.repository;


import com.kimwanyisacco.model.entity.Loan;
import com.kimwanyisacco.model.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByMemberId(Long memberId);

    /**
     * Used to enforce "a member may only hold one active loan at a time" -
     * checked before allowing a new application.
     */
    List<Loan> findByMemberIdAndStatusIn(Long memberId, List<LoanStatus> statuses);

    List<Loan> findByStatus(LoanStatus status);

    /** Supports "who has an overdue loan" without manual file review. */
    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, java.time.LocalDate date);
}
