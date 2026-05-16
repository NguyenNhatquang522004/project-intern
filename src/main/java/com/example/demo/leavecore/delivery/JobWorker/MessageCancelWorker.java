package com.example.demo.leavecore.delivery.JobWorker;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestResponse;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveUpdateStatusUseCase;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class MessageCancelWorker {
    private final ILeaveUpdateStatusUseCase leaveUpdateStatusUseCase;

    @JobWorker(type = "cancel-task")
    public void handleUpdateCancel(final JobClient client, final ActivatedJob job,
            @Variable String businessKey,
            @Variable String cancelReason) {
        log.info("--- START: handleUpdateCancel Worker ---");
        log.info("JobKey: {}, ProcessInstanceKey: {}, businessKey: {}, cancelReason: {}", job.getKey(), job.getProcessInstanceKey(), businessKey, cancelReason);
        try {
            if (businessKey == "" || businessKey == null) {
                log.info("businessKey is empty, extracting from variables");
                Map<String, Object> variables = job.getVariablesAsMap();
                log.info("Extracted Job Variables: {}", variables);
                LeaveRequestCreateRequest data = LeaveRequestRequest.mapToCreateRequest(variables);
                log.info("Mapped variables to LeaveRequestCreateRequest: {}", data);
                
                log.info("Calling leaveUpdateStatusUseCase.updateStatus with businessKey: {} status: REJECTED", data.businessKey());
                BaseResponse<LeaveRequestResponse> response = leaveUpdateStatusUseCase.updateStatus(data.businessKey(),
                        LeaveRequestStatusEnum.REJECTED);
                log.info("Received response: {}", response);
                if (response.getCode() != "200") {
                    log.warn("Update status failed. Throwing Error command.");
                    client.newThrowErrorCommand(job.getKey())
                            .errorCode(response.getCode())
                            .errorMessage(response.getMessage())
                            .send()
                            .join();
                    return;
                }
                final Map<String, Object> outputVariables = new HashMap<String, Object>();
                outputVariables.put("Message", "cancel leave request success");
                log.info("Setting output variables: {}", outputVariables);
                client.newCompleteCommand(job.getKey()).variables(outputVariables).send().join();
                log.info("--- END: handleUpdateCancel Worker successfully completed ---");
                return;
            }
            
            log.info("Calling leaveUpdateStatusUseCase.updateStatus with businessKey: {} status: REJECTED", businessKey);
            BaseResponse<LeaveRequestResponse> response = leaveUpdateStatusUseCase.updateStatus(businessKey,
                    LeaveRequestStatusEnum.REJECTED);
            log.info("Received response: {}", response);
            if (response.getCode() != "200") {
                log.warn("Update status failed. Throwing Error command.");
                client.newThrowErrorCommand(job.getKey())
                        .errorCode(response.getCode())
                        .errorMessage(response.getMessage())
                        .send()
                        .join();
                return;
            }
            final Map<String, Object> outputVariables = new HashMap<String, Object>();
            outputVariables.put("Message", "cancel leave request success");
            log.info("Setting output variables: {}", outputVariables);
            client.newCompleteCommand(job.getKey()).variables(outputVariables).send().join();
            log.info("--- END: handleUpdateCancel Worker successfully completed ---");

        } catch (Exception e) {
            log.error("Exception occurred in handleUpdateCancel Worker: {}", e.getMessage(), e);
            client.newFailCommand(job.getKey())
                    .retries(2)
                    .errorMessage(e.getMessage())
                    .send();
            log.info("Failed job: {} with remaining retries: 2", job.getKey());
        }
    }
}
