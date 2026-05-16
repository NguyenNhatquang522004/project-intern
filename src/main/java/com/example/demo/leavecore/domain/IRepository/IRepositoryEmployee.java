package com.example.demo.leavecore.domain.IRepository;

import com.example.demo.leavecore.domain.entity.Employee;
import java.util.Optional;
import java.util.UUID;

public interface IRepositoryEmployee {
    Employee save(Employee employee);
    Optional<Employee> findById(UUID id);
    void deleteById(UUID id);
    Optional<Employee> findByEmail(String email);
}
