package com.example.demo.leavecore.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.common.Enum.LeaveSessionEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leave_requests", indexes = {
        @Index(name = "idx_lr_employee_status", columnList = "employee_id, status"),
        @Index(name = "idx_lr_assignee_status", columnList = "current_assignee_id, status"),
        @Index(name = "idx_lr_dates", columnList = "start_date, end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "business_key", length = 100, nullable = false, unique = true)
    private String businessKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_assignee_id")
    private Employee currentAssignee;

    @Column(name = "current_assignee_name")
    private String currentAssigneeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_session", nullable = false)
    private LeaveSessionEnum leaveSession;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "total_working_days", precision = 4, scale = 1, nullable = false)
    private BigDecimal totalWorkingDays;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private LeaveRequestStatusEnum status;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
