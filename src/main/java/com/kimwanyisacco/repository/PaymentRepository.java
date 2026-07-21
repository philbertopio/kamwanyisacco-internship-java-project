package com.kimwanyisacco.repository;


import com.kimwanyisacco.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByMemberIdOrderByInitiatedAtDesc(Long memberId);
}
