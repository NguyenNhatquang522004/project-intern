package com.example.demo.leaveinfrastructure.usecase.adapterUseCase;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.common.grpc.CreateApprovalHistoryRequest;
import com.example.demo.leaveinfrastructure.domain.entity.ApprovalHistory;
import com.example.demo.leaveinfrastructure.usecase.IUseCase.IApprovalHistoryUseCase;
import com.example.demo.leaveinfrastructure.infrastructure.postgres.IRepositoryApprovalHistory;
import com.example.demo.leaveinfrastructure.infrastructure.postgres.Repository.ApprovalHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class ApprovalHistoryUseCase implements IApprovalHistoryUseCase {
    private final IRepositoryApprovalHistory approvalHistoryRepository;

    @Override
    public void recordApprovalHistory(CreateApprovalHistoryRequest request) {
        try {
            Optional<ApprovalHistory> approvalHistory = approvalHistoryRepository
                    .findByLeaveRequestId(UUID.fromString(request.getLeaveRequestId()));
            if (approvalHistory.isEmpty()) {
                ApprovalHistory approvalHistoryEntity = ApprovalHistory.builder()
                        .leaveRequestId(UUID.fromString(request.getLeaveRequestId()))
                        .approverEmail(request.getApproverEmail())
                        .level(request.getLevel())
                        .action(request.getAction())
                        .comment(request.getComment())
                        .build();
                approvalHistoryRepository.save(approvalHistoryEntity);
            } else {
                approvalHistory.get()
                        .setLevel(approvalHistory.get().getLevel() != request.getLevel() && request.getLevel() != 0
                                ? request.getLevel()
                                : approvalHistory.get()
                                        .getLevel());
                approvalHistory.get()
                        .setAction(request.getAction() != request.getAction() && request.getAction() != null
                                && request.getAction() != "" ? request.getAction() : approvalHistory.get().getAction());
                approvalHistory.get()
                        .setComment(request.getComment() != request.getComment() && request.getComment() != null
                                && request.getComment() != "" ? request.getComment()
                                        : approvalHistory.get().getComment());
                approvalHistory.get()
                        .setApproverEmail(request.getApproverEmail() != request.getApproverEmail()
                                && request.getApproverEmail() != null && request.getApproverEmail() != ""
                                        ? request.getApproverEmail()
                                        : approvalHistory.get().getApproverEmail());
                approvalHistoryRepository.save(approvalHistory.get());
                log.info("Approval history recorded successfully: {}", approvalHistory.get());
            }
            

        } catch (Exception e) {
            log.error("Error recording approval history: {}", e.getMessage());
            throw new RuntimeException("Error recording approval history", e);
        }

    }
}
