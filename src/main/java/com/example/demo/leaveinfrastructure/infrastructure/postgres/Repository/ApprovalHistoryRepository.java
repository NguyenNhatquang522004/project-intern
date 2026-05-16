package com.example.demo.leaveinfrastructure.infrastructure.postgres.Repository;

import com.example.demo.leaveinfrastructure.domain.entity.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, UUID> {
    Optional<ApprovalHistory> findByLeaveRequestId(UUID leaveRequestId);
}
