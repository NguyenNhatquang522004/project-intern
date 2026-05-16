package com.example.demo.leavecore.delivery.Dto.LeaveRequest;

import java.util.UUID;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {
    private UUID id;
    private String businessKey;
    private UUID employeeId;
    private UUID currentAssigneeId;
    private String currentAssigneeName;
    private UUID leaveTypeId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalWorkingDays;
    private String reason;
    private String status;
    private String attachmentUrl;
    private LocalDateTime updatedAt;
}
