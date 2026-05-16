package com.example.demo.leavecore.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.demo.common.Enum.DeparmentNameEnum;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments", indexes = {
        @Index(name = "idx_dept_manager", columnList = "manager_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeparmentNameEnum name;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", referencedColumnName = "id")
    private Employee manager;

    @Builder.Default
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();
}