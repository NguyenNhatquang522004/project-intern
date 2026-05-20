package com.example.demo.leavecore.usecase.adapterUseCase;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.leavecore.domain.IRepository.IRepositoryEmployee;
import com.example.demo.leavecore.domain.IRepository.IRepositoryLeaveRequest;
import com.example.demo.leavecore.domain.entity.Employee;
import com.example.demo.leavecore.domain.entity.LeaveRequest;
import com.example.demo.leavecore.usecase.IUseCase.IHandleTaskEventUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class HandleTaskEventUseCase implements IHandleTaskEventUseCase {
    private final IRepositoryLeaveRequest repositoryLeaveRequest;
    private final IRepositoryEmployee repositoryEmployee;

    @Override
    public void handleTaskAssignEvent(String bussinesskey, String emailAssignee) {
        try {
            log.info("handleTaskAssignEvent: {}", bussinesskey);
            Optional<Employee> employee = repositoryEmployee.findByEmail(emailAssignee);
            log.info("handleTaskAssignEvent: {}", employee.get().getEmail());
            if (employee.isEmpty()) {
                throw new RuntimeException("Employee not found");
            }
            Employee assignee = employee.get();
            Optional<LeaveRequest> leaveRequest = repositoryLeaveRequest.findByBusinessKey(bussinesskey);
            if (leaveRequest.isEmpty()) {
                throw new RuntimeException("Leave request not found");
            }
            LeaveRequest leaveRequestEntity = leaveRequest.get();
            leaveRequestEntity.setCurrentAssignee(assignee);
            leaveRequestEntity.setCurrentAssigneeName(assignee.getFullName());
            log.info("handleTaskAssignEvent: {}", leaveRequestEntity);
            repositoryLeaveRequest.save(leaveRequestEntity);
        } catch (Exception e) {
            log.error("handleTaskAssignEvent: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void handleTaskCompleteEvent(String bussinesskey, LeaveRequestStatusEnum status) {
        try {
            // log.info("handleTaskCompleteEvent: {}", bussinesskey);
            // Optional<LeaveRequest> leaveRequest = repositoryLeaveRequest.findById(UUID.fromString(bussinesskey));
            // if (leaveRequest.isEmpty()) {
            //     throw new RuntimeException("Leave request not found");
            // }
            // LeaveRequest leaveRequestEntity = leaveRequest.get();
            // leaveRequestEntity.setStatus(status);
            // leaveRequestEntity.setUpdatedAt(LocalDateTime.now());
            // repositoryLeaveRequest.save(leaveRequestEntity);
        } catch (Exception e) {
            log.error("handleTaskCompleteEvent: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

}
