package com.example.demo.leavecore.delivery.JobWorker;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.common.Enum.DeparmentNameEnum;
import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.common.Enum.LeaveTypeEnum;
import com.example.demo.common.grpc.ApprovalHistoryResponse;
import com.example.demo.common.grpc.CreateApprovalHistoryRequest;
import com.example.demo.common.grpc.LeaveApprovalServiceGrpc;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveValidRespones;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveValidateRequestUseCase;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Slf4j
@RequiredArgsConstructor
@Component
public class LeaveValidateRequestWorker {
    private final ILeaveValidateRequestUseCase leaveValidateRequestUseCase;
    @GrpcClient("leaveinfrastructure")
    private LeaveApprovalServiceGrpc.LeaveApprovalServiceBlockingStub leaveStub;

    @JobWorker(type = "validate-request")
    public void validateRequest(final JobClient client, final ActivatedJob job) {
        log.info("--- START: validateRequest Worker ---");
        log.info("JobKey: {}, ProcessInstanceKey: {}", job.getKey(), job.getProcessInstanceKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            log.info("Extracted Job Variables: {}", variables);
            
            LeaveRequestCreateRequest data = LeaveRequestRequest.mapToCreateRequest(variables);
            log.info("Mapped variables to LeaveRequestCreateRequest: {}", data);
            
            log.info("Calling leaveValidateRequestUseCase.validate");
            BaseResponse<LeaveValidRespones> response = leaveValidateRequestUseCase.validate(data);
            log.info("Received response: {}", response);

            if (!response.getData().getIsvalid()) {
                log.warn("Validation failed. Code: {}, Message: {}. Throwing Error command.", response.getCode(), response.getMessage());
                client.newThrowErrorCommand(job.getKey())
                        .errorCode(response.getCode())
                        .errorMessage(response.getMessage())
                        .send()
                        .join();
                return;
            }
            final Map<String, Object> outputVariables = new HashMap<String, Object>();
            outputVariables.put("isValid", response.getData().getIsvalid());
            outputVariables.put("TotalWorkingDays", response.getData().getTotalWorkingDays());
            outputVariables.put("BusinessKey", response.getData().getBusinessKey());
            outputVariables.put("Message", response.getData().getMessage());
            log.info("Prepared outputVariables: {}", outputVariables);
            
            log.info("Preparing CreateApprovalHistoryRequest");
            CreateApprovalHistoryRequest request = CreateApprovalHistoryRequest.newBuilder()
                    .setAction("validate-request")
                    .setLeaveRequestId(response.getData().getBusinessKey())
                    .setApproverEmail("")
                    .setLevel(0)
                    .setComment("Validate leave request").build();
            log.info("Calling leaveStub.recordApprovalHistory with request: {}", request);
            ApprovalHistoryResponse respone = leaveStub.recordApprovalHistory(request);
            log.info("Received approval history response: {}", respone);
            
            if (respone.getLeaveRequestId().equals(request.getLeaveRequestId())) {
                log.info("Approval history recorded successfully");
            } else {
                log.error("Error recording approval history");
                client.newThrowErrorCommand(job.getKey())
                        .errorCode("APPROVAL_HISTORY_ERROR")
                        .errorMessage("Error recording approval history")
                        .send()
                        .join();
                return;
            }
            log.info("Setting output variables and completing job: {}", job.getKey());
            client.newCompleteCommand(job.getKey()).variables(outputVariables).send().join();
            log.info("--- END: validateRequest Worker successfully completed ---");
        } catch (Exception e) {
            log.error("Exception occurred in validateRequest Worker: {}", e.getMessage(), e);
            client.newFailCommand(job.getKey()).retries(job.getRetries() - 1).errorMessage(e.getMessage())
                    .send().join();
            log.info("Failed job: {} with remaining retries: {}", job.getKey(), job.getRetries() - 1);
        }

    }

}
