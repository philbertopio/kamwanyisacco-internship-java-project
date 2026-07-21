package com.kimwanyisacco.repository;

import com.kimwanyisacco.model.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByNationalId(String nationalId);

    Optional<Member> findByMembershipNumber(String membershipNumber);

    boolean existsByNationalId(String nationalId);

    boolean existsByMembershipNumber(String membershipNumber);

    Optional<Member> findByUserUsername(String username);
}
