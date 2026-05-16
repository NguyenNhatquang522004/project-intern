package com.example.demo.leavecore.delivery.Dto.LeaveBalance;

import java.util.UUID;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {
    private UUID id;
    private UUID employeeId;
    private UUID leaveTypeId;
    private Integer year;
    private BigDecimal totalDays;
    private BigDecimal usedDays;
    private BigDecimal pendingDays;
    private Integer version;
}
