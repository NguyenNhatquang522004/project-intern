package com.example.demo.leavecore.delivery.Dto.Employee;

import java.util.UUID;
import java.time.LocalDateTime;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private UUID id;
    private String fullName;
    private String email;
    private Integer positionLevel;
    private UUID departmentId;
    private UUID managerId;
    private String status;
    private LocalDateTime createdAt;
}
