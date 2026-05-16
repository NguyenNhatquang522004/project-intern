package com.example.demo.leavecore.delivery.JobWorker;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.usecase.IUseCase.IHandleTaskEventUseCase;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class TaskCompleteWorker {
    private final IHandleTaskEventUseCase handleTaskEventUseCase;

    @JobWorker(type = "task-complete")
    public void completeTask(final JobClient client, final ActivatedJob job) {
        log.info("--- START: completeTask Worker ---");
        log.info("JobKey: {}, ProcessInstanceKey: {}", job.getKey(), job.getProcessInstanceKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            log.info("Extracted Job Variables: {}", variables);
            
            LeaveRequestCreateRequest data = LeaveRequestRequest.mapToCreateRequest(variables);
            log.info("Mapped variables to LeaveRequestCreateRequest: {}", data);
            
            String eventType = job.getCustomHeaders().get("zeebe:taskListenerEventType");
            log.info("Extracted eventType: {}", eventType);
            
            if (!eventType.equalsIgnoreCase("completing")) {
                log.warn("eventType is not completing. Throwing Error command.");
                client.newThrowErrorCommand(job.getKey())
                        .errorCode("400")
                        .errorMessage("Task is not completed")
                        .send()
                        .join();
                return;
            }
            if (data.status() == LeaveRequestStatusEnum.REJECTED) {
                log.info("Request status is REJECTED. Setting output variable Message: 'Từ chối phê duyệt'");
                final Map<String, Object> outputVariables = new HashMap<String, Object>();
                outputVariables.put("Message", "Từ chối phê duyệt");
                client.newCompleteCommand(job.getKey()).variables(outputVariables).send().join();
                log.info("--- END: completeTask Worker successfully completed with REJECTED status ---");
                return;
            }

            // gọi module cập nhật lịch sử
            log.info("Completing job normally");
            client.newCompleteCommand(job.getKey()).send().join();
            log.info("--- END: completeTask Worker successfully completed ---");
        } catch (Exception e) {
            log.error("Exception occurred in completeTask Worker: {}", e.getMessage(), e);
            client.newFailCommand(job.getKey()).retries(job.getRetries() - 1).errorMessage(e.getMessage())
                    .send().join();
            log.info("Failed job: {} with remaining retries: {}", job.getKey(), job.getRetries() - 1);
        }
    }
}
