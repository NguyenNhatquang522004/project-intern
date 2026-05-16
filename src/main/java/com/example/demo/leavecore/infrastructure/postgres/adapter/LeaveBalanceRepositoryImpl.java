package com.example.demo.leavecore.infrastructure.postgres.adapter;

import com.example.demo.leavecore.domain.IRepository.IRepositoryLeaveBalance;
import com.example.demo.leavecore.domain.entity.LeaveBalance;
import com.example.demo.leavecore.infrastructure.postgres.Repository.LeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LeaveBalanceRepositoryImpl implements IRepositoryLeaveBalance {

    private final LeaveBalanceRepository repository;

    @Override
    public LeaveBalance save(LeaveBalance leaveBalance) {
        return repository.save(leaveBalance);
    }

    @Override
    public Optional<LeaveBalance> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public List<LeaveBalance> findByEmployee_IdAndYear(UUID employeeId, Integer year) {
        return repository.findByEmployee_IdAndYear(employeeId, year);
    }

    @Override
    public List<LeaveBalance> findByEmployee_Id(UUID employeeID) {
        return repository.findByEmployee_Id(employeeID);
    }
}
