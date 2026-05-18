package com.example.demo.leavecore.delivery.Dto.manualtask;

import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import jakarta.validation.constraints.NotNull;

public record TaskCompleteRequestDto(
        @NotNull(message = "Trạng thái phê duyệt không được để trống")
        LeaveRequestStatusEnum status,

        String assignee
) {}
