package com.kimwanyisacco.repository;


import com.kimwanyisacco.model.entity.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {

    List<LoanRepayment> findByLoanIdOrderByRepaymentDateDesc(Long loanId);
}
