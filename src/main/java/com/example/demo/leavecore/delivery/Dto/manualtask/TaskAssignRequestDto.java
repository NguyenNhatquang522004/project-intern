package com.example.demo.leavecore.delivery.Dto.manualtask;

import jakarta.validation.constraints.NotBlank;

public record TaskAssignRequestDto(
        @NotBlank(message = "Assignee không được để trống")
        String assignee
) {}
