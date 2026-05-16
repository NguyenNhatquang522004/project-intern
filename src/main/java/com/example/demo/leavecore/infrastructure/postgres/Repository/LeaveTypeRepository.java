package com.example.demo.leavecore.infrastructure.postgres.Repository;

import com.example.demo.common.Enum.LeaveTypeEnum;
import com.example.demo.leavecore.domain.entity.LeaveType;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
    Optional<LeaveType> findByIsPaid(LeaveTypeEnum type);
}
