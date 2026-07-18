package com.kimwanyisacco.repository;

import com.kimwanyisacco.model.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByStaffId(String staffId);

}
