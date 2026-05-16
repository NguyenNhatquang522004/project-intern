package com.example.demo.leaveinfrastructure.usecase.IUseCase;

import com.example.demo.common.grpc.CreateApprovalHistoryRequest;
import com.example.demo.leaveinfrastructure.domain.entity.ApprovalHistory;

public interface IApprovalHistoryUseCase {
 public void recordApprovalHistory(CreateApprovalHistoryRequest request);
}
