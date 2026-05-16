package com.example.demo.leavecore.domain.IRepository;

import com.example.demo.leavecore.domain.entity.LeaveBalance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRepositoryLeaveBalance {
    LeaveBalance save(LeaveBalance leaveBalance);

    Optional<LeaveBalance> findById(UUID id);

    List<LeaveBalance> findByEmployee_IdAndYear(UUID employeeId, Integer year);

    void deleteById(UUID id);

    List<LeaveBalance> findByEmployee_Id(UUID employeeID);

}
