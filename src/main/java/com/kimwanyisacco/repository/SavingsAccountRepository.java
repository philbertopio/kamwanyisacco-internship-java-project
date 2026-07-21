package com.kimwanyisacco.repository;

import com.kimwanyisacco.model.entity.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {

    Optional<SavingsAccount> findByMemberId(Long memberId);
}
