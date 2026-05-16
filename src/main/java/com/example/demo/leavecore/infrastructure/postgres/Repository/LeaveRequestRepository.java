package com.example.demo.leavecore.infrastructure.postgres.Repository;

import com.example.demo.leavecore.domain.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    Optional<LeaveRequest> findByBusinessKey(String businessKey);
}
