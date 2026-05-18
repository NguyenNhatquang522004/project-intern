package com.example.demo.leaveinfrastructure.delivery.grpc;

import org.springframework.stereotype.Component;

import com.example.demo.common.grpc.ApprovalHistoryResponse;
import com.example.demo.common.grpc.CreateApprovalHistoryRequest;
import com.example.demo.common.grpc.LeaveApprovalServiceGrpc.LeaveApprovalServiceImplBase;
import com.example.demo.leaveinfrastructure.usecase.IUseCase.IApprovalHistoryUseCase;

import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@RequiredArgsConstructor
@GrpcService
public class LeaveApprovalGrpc extends LeaveApprovalServiceImplBase {
    private final IApprovalHistoryUseCase approvalHistoryUseCase;

    @Override
    public void recordApprovalHistory(CreateApprovalHistoryRequest request,
            io.grpc.stub.StreamObserver<ApprovalHistoryResponse> responseObserver) {
        try {
            approvalHistoryUseCase.recordApprovalHistory(request);

            responseObserver.onNext(ApprovalHistoryResponse.newBuilder()
                    .setLeaveRequestId(request.getLeaveRequestId())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error recording approval history: {}", e.getMessage());

            // 💡 ĐÃ SỬA: Thay vì set `null` gây lỗi sập, ta ném lỗi chuẩn gRPC về cho
            // Client (leavecore) nhận biết
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Lỗi hệ thống tại LeaveInfrastructure: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}