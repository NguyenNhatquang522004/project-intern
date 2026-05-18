package com.example.demo.leavecore.delivery.Dto.ManualTrigger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.demo.common.Enum.DeparmentNameEnum;
import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.common.Enum.LeaveSessionEnum;
import com.example.demo.common.Enum.LeaveTypeEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ValidateRequestInput(
        @NotBlank(message = "Business Key không được để trống") String businessKey,
        @NotBlank(message = "Email không được để trống") String email,
        @NotNull(message = "ID Nhân viên không được để trống") UUID employeeId,
        @NotNull(message = "Department không được để trống") DeparmentNameEnum departmentName,
        @NotNull(message = "Loại nghỉ không được để trống") LeaveTypeEnum leaveType,
        @NotNull(message = "Ca nghỉ không được để trống") LeaveSessionEnum leaveSession,
        @NotNull(message = "Ngày bắt đầu không được để trống") LocalDateTime startDate,
        @NotNull(message = "Ngày kết thúc không được để trống") LocalDateTime endDate,
        @NotNull(message = "Tổng số ngày làm việc không được để trống") BigDecimal totalWorkingDays,
        @NotBlank(message = "Tên nhân viên không được để trống") String fullName,
        String reason,
        LeaveRequestStatusEnum status,
        String attachmentUrl
) {}
