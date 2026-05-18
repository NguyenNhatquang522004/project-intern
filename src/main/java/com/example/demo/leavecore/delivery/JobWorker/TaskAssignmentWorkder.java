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

    @JobWorker(type = "task-assign", autoComplete = false)
    public void assignTask(final JobClient client, final ActivatedJob job, @Variable String assignee) {
        log.info("--- START: assignTask Worker ---");
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Map<String, String> headers = job.getCustomHeaders();

            // 1. Trích xuất chính xác theo Key thực tế từ Log
            String eventAction = headers.get("io.camunda.zeebe:action"); // Sẽ lấy được chữ "assign"
            String realAssignee = headers.get("io.camunda.zeebe:assignee"); // Sẽ lấy được chữ "demo"

            log.info("Extracted eventAction: {}, realAssignee: {}", eventAction, realAssignee);

            LeaveRequestCreateRequest data = LeaveRequestRequest.mapToCreateRequest(variables);

            // 2. Kiểm tra trạng thái bằng biến eventAction mới
            if (eventAction == null || !"assign".equalsIgnoreCase(eventAction)) {
                log.warn("eventAction không hợp lệ hoặc không phải là 'assign'.");

                // Đã đổi THROW_ERROR thành FAIL_COMMAND để không bị crash INVALID_STATE
                client.newFailCommand(job.getKey())
                        .retries(0)
                        .errorMessage("Invalid task listener action state")
                        .send()
                        .join();
                return;
            }

            // 3. Xử lý nghiệp vụ (Sử dụng realAssignee thay vì biến assignee bị null)
            log.info("Calling handleTaskEventUseCase.handleTaskAssignEvent with businessKey: {}, assignee: {}",
                    data.businessKey(), realAssignee);

            handleTaskEventUseCase.handleTaskAssignEvent(data.businessKey(), realAssignee);
            log.info("Task assigned successfully");

            // 4. Xác nhận hoàn thành với Camunda
            client.newCompleteCommand(job.getKey()).send().join();
            log.info("--- END: assignTask Worker successfully completed ---");

        } catch (Exception e) {
            log.error("Exception occurred in assignTask Worker: {}", e.getMessage(), e);
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage(e.getMessage())
                    .send()
                    .join();
        }
    }

}
