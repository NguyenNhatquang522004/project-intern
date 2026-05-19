package com.example.demo.leavecore.infrastructure.postgres.adapter;

import com.example.demo.leavecore.domain.IRepository.IRepositoryEmployee;
import com.example.demo.leavecore.domain.entity.Employee;
import com.example.demo.leavecore.infrastructure.postgres.Repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmployeeRepositoryImpl implements IRepositoryEmployee {

    private final EmployeeRepository repository;

    @Override
    public Employee save(Employee employee) {
        return repository.save(employee);
    }

    @Override
    public Optional<Employee> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        return repository.findByEmail(email);
    }

    @Override
    public void DeleteEmployeeByEmail(String email) {
        repository.deleteByEmail(email);
    }
}
