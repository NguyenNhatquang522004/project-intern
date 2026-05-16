package com.example.demo.leaveinfrastructure.infrastructure.postgres.adapter;

import com.example.demo.leaveinfrastructure.domain.entity.ApprovalHistory;
import com.example.demo.leaveinfrastructure.infrastructure.postgres.IRepositoryApprovalHistory;
import com.example.demo.leaveinfrastructure.infrastructure.postgres.Repository.ApprovalHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApprovalHistoryRepositoryImpl implements IRepositoryApprovalHistory {

    private final ApprovalHistoryRepository repository;

    @Override
    public ApprovalHistory save(ApprovalHistory approvalHistory) {
        return repository.save(approvalHistory);
    }

    @Override
    public Optional<ApprovalHistory> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<ApprovalHistory> findByLeaveRequestId(UUID leaveRequestId) {
        return repository.findByLeaveRequestId(leaveRequestId);
    }
}
