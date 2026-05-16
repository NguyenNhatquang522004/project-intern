package com.example.demo.leavecore.domain.IRepository;

import com.example.demo.common.Enum.LeaveTypeEnum;
import com.example.demo.leavecore.domain.entity.LeaveType;
import java.util.Optional;
import java.util.UUID;

public interface IRepositoryLeaveType {
    LeaveType save(LeaveType leaveType);

    Optional<LeaveType> findById(UUID id);

    Optional<LeaveType> findByType(LeaveTypeEnum type);

    void deleteById(UUID id);
}
