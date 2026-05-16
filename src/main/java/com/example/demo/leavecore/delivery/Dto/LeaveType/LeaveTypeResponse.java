package com.example.demo.leavecore.delivery.Dto.LeaveType;

import java.util.UUID;

import com.example.demo.common.Enum.LeaveTypeEnum;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeResponse {
    private UUID id;
    private String code;
    private String name;
    private Boolean requiresProof;
    private LeaveTypeEnum isPaid;
}
