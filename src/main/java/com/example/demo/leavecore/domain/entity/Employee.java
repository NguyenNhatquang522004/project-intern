package com.example.demo.leavecore.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.example.demo.common.Enum.AccountStatusEnum;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "employees", indexes = {
        @Index(name = "idx_emp_department", columnList = "department_id"),
        @Index(name = "idx_emp_manager", columnList = "manager_id"),
        @Index(name = "idx_emp_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "OTP", nullable = true)
    private String codeOTP;

    @Column(name = "OTP_EXPIRY_DATE", nullable = true)
    private LocalDateTime codeOtpExpiryDate;

    @Column(name = "count_resend_email")
    private Integer countResendEmail;

    @Column(name = "count_fail_otp")
    private Integer countFailOtp;

    @Column(name = "position_level", nullable = false)
    private Integer positionLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(name = "account_status", length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private AccountStatusEnum status = AccountStatusEnum.INACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Employee> subordinates = new ArrayList<>();
}
