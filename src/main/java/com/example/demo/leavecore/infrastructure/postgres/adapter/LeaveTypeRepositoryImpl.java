package com.example.demo.leavecore.infrastructure.postgres.adapter;

import com.example.demo.common.Enum.LeaveTypeEnum;
import com.example.demo.leavecore.domain.IRepository.IRepositoryLeaveType;
import com.example.demo.leavecore.domain.entity.LeaveType;
import com.example.demo.leavecore.infrastructure.postgres.Repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LeaveTypeRepositoryImpl implements IRepositoryLeaveType {

    private final LeaveTypeRepository repository;

    @Override
    public LeaveType save(LeaveType leaveType) {
        return repository.save(leaveType);
    }

    @Override
    public Optional<LeaveType> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<LeaveType> findByType(LeaveTypeEnum type) {
        return repository.findByIsPaid(type);
    }
}
