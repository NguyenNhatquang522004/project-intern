package com.example.demo.leavecore.delivery.Dto.ManualTrigger;

import com.example.demo.common.Enum.LeaveRequestStatusEnum;

public record UpdateStatusOutput(
        LeaveRequestStatusEnum status,
        Boolean need0,
        Boolean isvaildrule,
        String message
) {}
