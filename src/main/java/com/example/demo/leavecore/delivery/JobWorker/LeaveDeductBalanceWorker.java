package com.example.demo.leavecore.delivery.JobWorker;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestResponse;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveDeductBalanceUseCase;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class LeaveDeductBalanceWorker {
    private final ILeaveDeductBalanceUseCase leaveDeductBalanceUseCase;
    
    @JobWorker(type = "deduct-balance-task")
    public void deductBalance(final JobClient client, final ActivatedJob job) {
        log.info("--- START: deductBalance Worker ---");
        log.info("JobKey: {}, ProcessInstanceKey: {}", job.getKey(), job.getProcessInstanceKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            log.info("Extracted Job Variables: {}", variables);
            
            LeaveRequestCreateRequest data = LeaveRequestRequest.mapToCreateRequest(variables);
            log.info("Mapped variables to LeaveRequestCreateRequest: {}", data);
            
            log.info("Calling leaveDeductBalanceUseCase.deductBalance with businessKey: {}", data.businessKey());
            BaseResponse<LeaveRequestResponse> response = leaveDeductBalanceUseCase.deductBalance(data.businessKey());
            log.info("Received response from leaveDeductBalanceUseCase: {}", response);
            
            if (response.getCode() != "200") {
                log.warn("Deduct balance failed. Code: {}, Message: {}. Throwing BPMN Error.", response.getCode(), response.getMessage());
                client.newThrowErrorCommand(job.getKey())
                        .errorCode(response.getCode())
                        .errorMessage(response.getMessage())
                        .send()
                        .join();
                return;
            }
            final Map<String, Object> outputVariables = new HashMap<String, Object>();
            outputVariables.put("Message", "Leave request approved and balance deducted successfully");
            log.info("Setting output variables: {}", outputVariables);
            
            client.newCompleteCommand(job.getKey()).variables(outputVariables).send().join();
            log.info("--- END: deductBalance Worker successfully completed ---");
        } catch (Exception e) {
            log.error("Exception occurred in deductBalance Worker: {}", e.getMessage(), e);
            client.newFailCommand(job.getKey()).retries(job.getRetries() - 1).errorMessage(e.getMessage())
                    .send().join();
            log.info("Failed job: {} with remaining retries: {}", job.getKey(), job.getRetries() - 1);
        }
    }

}
