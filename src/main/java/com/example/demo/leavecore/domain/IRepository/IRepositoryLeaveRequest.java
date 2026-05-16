package com.example.demo.leavecore.domain.IRepository;

import com.example.demo.leavecore.domain.entity.LeaveRequest;
import java.util.Optional;
import java.util.UUID;

public interface IRepositoryLeaveRequest {
    LeaveRequest save(LeaveRequest leaveRequest);

    Optional<LeaveRequest> findById(UUID id);

    void deleteById(UUID id);

    Optional<LeaveRequest> findByBusinessKey(String businessKey);
}
