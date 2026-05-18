package com.example.demo.leavecore.delivery.Dto.ManualTrigger;

import jakarta.validation.constraints.NotBlank;

public record TaskAssignmentInput(
        @NotBlank(message = "Business Key không được để trống") String businessKey,
        @NotBlank(message = "Assignee không được để trống") String assignee
) {}
