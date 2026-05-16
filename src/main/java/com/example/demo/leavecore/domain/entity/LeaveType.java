package com.example.demo.leavecore.domain.entity;

import java.util.UUID;

import com.example.demo.common.Enum.LeaveTypeEnum;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "requires_proof")
    @Builder.Default
    private Boolean requiresProof = false;

    @Column(name = "is_paid")
    @Enumerated(EnumType.STRING)
    private LeaveTypeEnum isPaid;
}
