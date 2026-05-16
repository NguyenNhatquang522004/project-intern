package com.example.demo.leavecore.infrastructure.postgres.Repository;

import com.example.demo.leavecore.domain.entity.Employee;
import com.example.demo.leavecore.domain.entity.LeaveBalance;
import com.example.demo.leavecore.domain.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    List<LeaveBalance> findByEmployee_IdAndYear(UUID employeeId, Integer year);

    List<LeaveBalance> findByEmployee_Id(UUID employeeID);

    Optional<LeaveBalance> findByEmployeeAndLeaveTypeAndYear(Employee employee, LeaveType leaveType, Integer year);
}
