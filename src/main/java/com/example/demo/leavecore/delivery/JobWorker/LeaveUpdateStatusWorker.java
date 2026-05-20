package com.example.demo.leavecore.delivery.JobWorker;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestResponse;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveUpdateStatusUseCase;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class LeaveUpdateStatusWorker {
    private final ILeaveUpdateStatusUseCase leaveUpdateStatusUseCase;

    @JobWorker(type = "Update-request-pending")
    public Map<String, Object> updateStatus(final JobClient client, final ActivatedJob job) {
        log.info("--- START: updateStatus Worker ---");
        log.info("JobKey: {}, ProcessInstanceKey: {}", job.getKey(), job.getProcessInstanceKey());
        final Map<String, Object> outputVariables = new HashMap<String, Object>();
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            log.info("Extracted Job Variables: {}", variables);

            LeaveRequestCreateRequest data = LeaveRequestRequest.mapToCreateRequest(variables);
            log.info("Mapped variables to LeaveRequestCreateRequest: {}", data);

            log.info("Calling leaveUpdateStatusUseCase.updateStatus with businessKey: {}, status: {}",
                    data.businessKey(), data.status());
            BaseResponse<LeaveRequestResponse> response = leaveUpdateStatusUseCase.updateStatus(data.businessKey(),
                    data.status());
            log.info("Received response: {}", response);

            if (response.getCode() != "200") {
                log.warn("Update status failed. Code: {}, Message: {}. Throwing Fail command.", response.getCode(),
                        response.getMessage());
                client.newFailCommand(job.getKey()).retries(job.getRetries() - 1).errorMessage(response.getMessage());
            }

            
    
            outputVariables.put("need0", false);
            outputVariables.put("isvaildrule", true);
            outputVariables.put("Status", data.status());
            log.info("Setting output variables: {}", outputVariables);

            client.newCompleteCommand(job.getKey()).variables(outputVariables).send().join();
            log.info("--- END: updateStatus Worker successfully completed ---");
            return outputVariables;

        } catch (Exception e) {
            log.error("Exception occurred in updateStatus Worker: {}", e.getMessage(), e);
            client.newFailCommand(job.getKey()).retries(job.getRetries() - 1).errorMessage(e.getMessage())
                    .send().join();
            log.info("Failed job: {} with remaining retries: {}", job.getKey(), job.getRetries() - 1);
            return outputVariables;
        }

    }

}
