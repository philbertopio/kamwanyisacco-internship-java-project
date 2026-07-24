package com.kimwanyisacco.repository;

import com.kimwanyisacco.model.entity.MemberTransactionApproval;
import com.kimwanyisacco.model.enums.MemberTransactionApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberTransactionApprovalRepository extends JpaRepository<MemberTransactionApproval, Long> {

    @Query("select request from MemberTransactionApproval request "
            + "join fetch request.member "
            + "left join fetch request.savingsAccount "
            + "left join fetch request.loan "
            + "where request.status = :status order by request.requestedAt asc")
    List<MemberTransactionApproval> findPendingWithTargets(@Param("status") MemberTransactionApprovalStatus status);
}
