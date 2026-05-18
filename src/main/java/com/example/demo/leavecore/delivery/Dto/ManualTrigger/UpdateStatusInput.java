package com.example.demo.leavecore.delivery.Dto.ManualTrigger;

import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusInput(
        @NotBlank(message = "Business Key không được để trống") String businessKey,
        @NotNull(message = "Trạng thái không được để trống") LeaveRequestStatusEnum status
) {}
