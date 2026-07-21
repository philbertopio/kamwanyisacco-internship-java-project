package com.kimwanyisacco.repository;

import com.kimwanyisacco.model.entity.SavingsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Long> {

    List<SavingsTransaction> findByAccountIdOrderByTransactionDateDesc(Long accountId);
}
