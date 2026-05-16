package com.example.demo.leavecore.delivery.Dto.LeaveBalance;

import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LeaveBalanceRequest {
    public record LeaveBalanceCreateRequest(
        @NotNull(message = "ID Nhân viên không được để trống")
        UUID employeeId,

        @NotNull(message = "ID Loại nghỉ không được để trống")
        Integer leaveTypeId,

        @NotNull(message = "Năm không được để trống")
        Integer year,

        @NotNull(message = "Tổng số ngày không được để trống")
        BigDecimal totalDays
    ) {}

    public record LeaveBalanceUpdateRequest(
        @NotNull(message = "ID Nhân viên không được để trống")
        UUID employeeId,

        @NotNull(message = "ID Loại nghỉ không được để trống")
        Integer leaveTypeId,

        @NotNull(message = "Năm không được để trống")
        Integer year,

        @NotNull(message = "Tổng số ngày không được để trống")
        BigDecimal totalDays
    ) {}
}
