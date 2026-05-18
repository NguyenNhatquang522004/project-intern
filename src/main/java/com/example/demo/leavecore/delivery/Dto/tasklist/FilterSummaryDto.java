package com.example.demo.leavecore.delivery.Dto.tasklist;

public record FilterSummaryDto(
        long allOpen,
        long assignedToMe,
        long unassigned,
        long completed
) {}
