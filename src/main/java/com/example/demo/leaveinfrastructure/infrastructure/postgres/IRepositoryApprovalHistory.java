package com.example.demo.leaveinfrastructure.infrastructure.postgres;

import com.example.demo.leaveinfrastructure.domain.entity.ApprovalHistory;
import java.util.Optional;
import java.util.UUID;

public interface IRepositoryApprovalHistory {
    ApprovalHistory save(ApprovalHistory approvalHistory);

    Optional<ApprovalHistory> findById(UUID id);

    Optional<ApprovalHistory> findByLeaveRequestId(UUID leaveRequestId);
    
}
