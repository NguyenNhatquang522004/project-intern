package com.example.demo.leavecore.delivery.Dto.ManualTrigger;

import jakarta.validation.constraints.NotBlank;

public record NotificationInput(
        @NotBlank(message = "Email không được để trống") String email,
        @NotBlank(message = "Trạng thái không được để trống") String status,
        @NotBlank(message = "Tổng số ngày không được để trống") String totalWorkingDays,
        String message
) {}
