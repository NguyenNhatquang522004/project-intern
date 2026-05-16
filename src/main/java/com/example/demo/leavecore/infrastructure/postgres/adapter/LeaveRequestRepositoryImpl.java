package com.example.demo.leavecore.infrastructure.postgres.adapter;

import com.example.demo.leavecore.domain.IRepository.IRepositoryLeaveRequest;
import com.example.demo.leavecore.domain.entity.LeaveRequest;
import com.example.demo.leavecore.infrastructure.postgres.Repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LeaveRequestRepositoryImpl implements IRepositoryLeaveRequest {

    private final LeaveRequestRepository repository;

    @Override
    public LeaveRequest save(LeaveRequest leaveRequest) {
        return repository.save(leaveRequest);
    }

    @Override
    public Optional<LeaveRequest> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<LeaveRequest> findByBusinessKey(String businessKey) {
        return repository.findByBusinessKey(businessKey);
    }
}
