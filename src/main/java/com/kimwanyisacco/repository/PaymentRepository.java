package com.kimwanyisacco.repository;

import com.kimwanyisacco.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByMemberIdOrderByInitiatedAtDesc(Long memberId);

    /** Used by IPN handler and verify endpoint to locate a payment by our reference. */
    Optional<Payment> findByMerchantReference(String merchantReference);

    /** Fallback lookup using PesaPal's own tracking id. */
    Optional<Payment> findByOrderTrackingId(String orderTrackingId);
}
