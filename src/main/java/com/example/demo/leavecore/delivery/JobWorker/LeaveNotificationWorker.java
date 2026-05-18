package com.example.demo.leavecore.delivery.JobWorker;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveNotificationUseCase;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class LeaveNotificationWorker {
    private final ILeaveNotificationUseCase leaveNotificationUseCase;

    @JobWorker(type = "notify")
    public void sendLeaveStatusUpdateEmail(final JobClient client, final ActivatedJob job) {
        log.info("--- START: sendLeaveStatusUpdateEmail Worker ---");
        log.info("JobKey: {}, ProcessInstanceKey: {}", job.getKey(), job.getProcessInstanceKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            log.info("Extracted Job Variables: {}", variables);

            LeaveRequestCreateRequest data = LeaveRequestRequest.mapToCreateRequest(variables);
            log.info("Mapped variables to LeaveRequestCreateRequest: {}", data);

            String message = variables.get("message") != null ? variables.get("message").toString() : "";
            log.info("Extracted message from variables: '{}'", message);

            log.info(
                    "Calling leaveNotificationUseCase.sendLeaveStatusUpdateEmail with employeeId: {}, status: {}, totalWorkingDays: {}, message: {}",
                    data.employeeId(), data.status(), data.totalWorkingDays(), message);
            leaveNotificationUseCase.sendLeaveStatusUpdateEmail(data.email(), data.status().toString(),
                    data.totalWorkingDays().toString(), message);
            log.info("Successfully sent leave status update email.");

            client.newCompleteCommand(job.getKey()).send().join();
            log.info("--- END: sendLeaveStatusUpdateEmail Worker successfully completed ---");
        } catch (Exception e) {
            log.error("Exception occurred in sendLeaveStatusUpdateEmail Worker: {}", e.getMessage(), e);
            client.newFailCommand(job.getKey()).retries(job.getRetries() - 1).errorMessage(e.getMessage())
                    .send().join();
            log.info("Failed job: {} with remaining retries: {}", job.getKey(), job.getRetries() - 1);
        }
    }

}
