package com.example.demo.leavecore.delivery.Dto.LeaveRequest;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaveValidRespones {
    private Boolean isvalid;
    private BigDecimal totalWorkingDays;
    private String businessKey;
    private String message;
    private String error;
}
