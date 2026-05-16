package com.example.demo.leavecore.infrastructure.postgres.Repository;

import com.example.demo.common.Enum.DeparmentNameEnum;
import com.example.demo.leavecore.domain.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByCode(String code);

    Optional<Department> findByName(DeparmentNameEnum name);

    boolean existsByCode(String code);
}
