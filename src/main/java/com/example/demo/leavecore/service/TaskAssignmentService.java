package com.example.demo.leavecore.service;

import org.springframework.stereotype.Service;

import com.example.demo.leavecore.delivery.Dto.ManualTrigger.TaskAssignmentInput;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.TaskAssignmentOutput;
import com.example.demo.leavecore.usecase.IUseCase.IHandleTaskEventUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class TaskAssignmentService {

    private final IHandleTaskEventUseCase handleTaskEventUseCase;

    public TaskAssignmentOutput execute(TaskAssignmentInput input) {
        log.info("--- [SERVICE] Bắt đầu xử lý TaskAssignment: businessKey={}, assignee={} ---", 
                 input.businessKey(), input.assignee());

        try {
            handleTaskEventUseCase.handleTaskAssignEvent(input.businessKey(), input.assignee());
            log.info("Task assigned successfully in domain usecase");
            return new TaskAssignmentOutput(true, "Task assignment event handled successfully");
        } catch (Exception e) {
            log.error("Failed to handle task assignment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle task assignment: " + e.getMessage());
        }
    }
}
