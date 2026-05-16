package com.example.demo.leavecore.delivery.JobWorker;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.usecase.IUseCase.IHandleTaskEventUseCase;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class TaskAssignmentWorkder {
    private final IHandleTaskEventUseCase handleTaskEventUseCase;

    @JobWorker(type = "task-assign")
    public void assignTask(final JobClient client, final ActivatedJob job, @Variable String assignee) {
        log.info("--- START: assignTask Worker ---");
        log.info("JobKey: {}, ProcessInstanceKey: {}, assignee: {}", job.getKey(), job.getProcessInstanceKey(), assignee);
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            log.info("Extracted Job Variables: {}", variables);
            
            LeaveRequestCreateRequest data = LeaveRequestRequest.mapToCreateRequest(variables);
            log.info("Mapped variables to LeaveRequestCreateRequest: {}", data);
            
            String eventType = job.getCustomHeaders().get("zeebe:taskListenerEventType");
            log.info("Extracted eventType: {}", eventType);
            
            if (!eventType.equalsIgnoreCase("assigning")) {
                log.warn("eventType is not assigning. Throwing Error command.");
                client.newThrowErrorCommand(job.getKey())
                        .errorCode("400")
                        .errorMessage("Task is not assigned")
                        .send()
                        .join();
                return;
            }
            log.info("Calling handleTaskEventUseCase.handleTaskAssignEvent with businessKey: {}, assignee: {}", data.businessKey(), assignee);
            handleTaskEventUseCase.handleTaskAssignEvent(data.businessKey(), assignee);
            log.info("Task assigned successfully");
            
            client.newCompleteCommand(job.getKey()).send().join();
            log.info("--- END: assignTask Worker successfully completed ---");
        } catch (Exception e) {
            log.error("Exception occurred in assignTask Worker: {}", e.getMessage(), e);
            client.newFailCommand(job.getKey()).retries(job.getRetries() - 1).errorMessage(e.getMessage())
                    .send().join();
            log.info("Failed job: {} with remaining retries: {}", job.getKey(), job.getRetries() - 1);
        }
    }

}
