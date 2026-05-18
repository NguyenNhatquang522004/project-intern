package com.example.demo.leavecore.service;

import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import io.camunda.zeebe.client.ZeebeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service điều khiển trực tiếp vòng đời User Task (Assign/Complete) thông qua Zeebe Client.
 * Việc gọi API này của Camunda sẽ gián tiếp kích hoạt các Task Listener tương ứng của quy trình (task-assign và task-complete).
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserTaskTriggerService {

    private final ZeebeClient zeebeClient;

    /**
     * Thực hiện gán (Assign) User Task cho một Assignee trên Camunda Zeebe.
     * 
     * @param taskId ID của User Task (Task Key) trên Camunda
     * @param assignee Tên/Email của người được gán
     */
    public void assignTask(long taskId, String assignee) {
        log.info("--- [SERVICE] Bắt đầu gán User Task: taskId={}, assignee={} ---", taskId, assignee);
        try {
            zeebeClient.newUserTaskAssignCommand(taskId)
                    .assignee(assignee)
                    .send()
                    .join();
            log.info("--- [SERVICE] Đã gán thành công User Task trên Zeebe: taskId={} ---", taskId);
        } catch (Exception e) {
            log.error("--- [SERVICE] Gặp lỗi khi gán User Task trên Zeebe: taskId={}, error={} ---", taskId, e.getMessage(), e);
            throw new RuntimeException("Gán User Task thất bại. Chi tiết: " + e.getMessage(), e);
        }
    }

    /**
     * Thực hiện hoàn thành (Complete) User Task trên Camunda Zeebe với kết quả Phê duyệt/Từ chối.
     * 
     * @param taskId ID của User Task (Task Key) trên Camunda
     * @param status Trạng thái phê duyệt (APPROVED hoặc REJECTED)
     * @param assignee Tên/Email người phê duyệt (tùy chọn)
     */
    public void completeTask(long taskId, LeaveRequestStatusEnum status, String assignee) {
        log.info("--- [SERVICE] Bắt đầu hoàn thành User Task: taskId={}, status={}, assignee={} ---", taskId, status, assignee);
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("Status", status.name());
            if (assignee != null && !assignee.isBlank()) {
                variables.put("assignee", assignee);
            }

            zeebeClient.newUserTaskCompleteCommand(taskId)
                    .variables(variables)
                    .send()
                    .join();
            log.info("--- [SERVICE] Đã hoàn thành thành công User Task trên Zeebe: taskId={} ---", taskId);
        } catch (Exception e) {
            log.error("--- [SERVICE] Gặp lỗi khi hoàn thành User Task trên Zeebe: taskId={}, error={} ---", taskId, e.getMessage(), e);
            throw new RuntimeException("Hoàn thành User Task thất bại. Chi tiết: " + e.getMessage(), e);
        }
    }
}
