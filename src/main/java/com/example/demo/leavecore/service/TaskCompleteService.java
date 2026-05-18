package com.example.demo.leavecore.service;

import org.springframework.stereotype.Service;

import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.common.grpc.CreateApprovalHistoryRequest;
import com.example.demo.common.grpc.LeaveApprovalServiceGrpc.LeaveApprovalServiceBlockingStub;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.TaskCompleteInput;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.TaskCompleteOutput;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskCompleteService {

    @GrpcClient("leaveinfrastructure")
    private LeaveApprovalServiceBlockingStub leaveApprovalServiceBlockingStub;

    public TaskCompleteOutput execute(TaskCompleteInput input) {
        log.info("--- [SERVICE] Bắt đầu xử lý TaskComplete: businessKey={}, status={}, assignee={} ---", 
                 input.businessKey(), input.status(), input.assignee());

        try {
            String message;
            if (input.status() == LeaveRequestStatusEnum.REJECTED) {
                log.info("Status is REJECTED. Creating approval history request for reject.");
                message = "Từ chối phê duyệt";
                
                CreateApprovalHistoryRequest historyRequest = CreateApprovalHistoryRequest.newBuilder()
                        .setLeaveRequestId(input.businessKey())
                        .setApproverEmail(input.assignee() != null ? input.assignee() : "")
                        .setAction("reject")
                        .build();

                // Quá trình gọi gRPC có thể được kích hoạt nếu cấu hình môi trường gRPC sẵn sàng
                // log.info("Calling leaveApprovalServiceBlockingStub.recordApprovalHistory (reject)");
                // leaveApprovalServiceBlockingStub.recordApprovalHistory(historyRequest);
            } else {
                log.info("Status is APPROVED/other. Creating approval history request for approval.");
                message = "Chấp thuận phê duyệt";

                CreateApprovalHistoryRequest historyRequest = CreateApprovalHistoryRequest.newBuilder()
                        .setLeaveRequestId(input.businessKey())
                        .setApproverEmail(input.assignee() != null ? input.assignee() : "")
                        .setAction("approval")
                        .build();

                // log.info("Calling leaveApprovalServiceBlockingStub.recordApprovalHistory (approval)");
                // leaveApprovalServiceBlockingStub.recordApprovalHistory(historyRequest);
            }

            log.info("Task completion handled successfully in service: message='{}'", message);
            return new TaskCompleteOutput(input.status().toString(), message);
        } catch (Exception e) {
            log.error("Failed to handle task completion in service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle task completion: " + e.getMessage());
        }
    }
}
