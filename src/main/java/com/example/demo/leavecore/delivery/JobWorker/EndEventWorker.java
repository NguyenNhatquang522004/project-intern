package com.example.demo.leavecore.delivery.JobWorker;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class EndEventWorker {
    
    @JobWorker(type = "EndEvent")
    public void endError(final JobClient client, final ActivatedJob job) {
        log.info("--- START: endError Worker ---");
        log.info("JobKey: {}, ProcessInstanceKey: {}", job.getKey(), job.getProcessInstanceKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            log.info("Extracted Job Variables: {}", variables);
            
            LeaveRequestCreateRequest data = LeaveRequestRequest.mapToCreateRequest(variables);
            log.info("Mapped variables to LeaveRequestCreateRequest: {}", data);
            
            log.info("Completing job: {}", job.getKey());
            client.newCompleteCommand(job.getKey()).send().join();
            log.info("--- END: endError Worker successfully completed ---");
        } catch (Exception e) {
            log.error("Exception occurred in endError Worker: {}", e.getMessage(), e);
            client.newFailCommand(job.getKey()).retries(job.getRetries() - 1).errorMessage(e.getMessage())
                    .send().join();
            log.info("Failed job: {} with remaining retries: {}", job.getKey(), job.getRetries() - 1);
        }
    }

    
    
}
