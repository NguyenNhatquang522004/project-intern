package com.example.demo.leavecore.delivery.Dto.LeaveRequest;

import java.util.Map;
import java.util.UUID;

import com.example.demo.common.Enum.DeparmentNameEnum;
import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.common.Enum.LeaveSessionEnum;
import com.example.demo.common.Enum.LeaveTypeEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LeaveRequestRequest {
        public static LeaveRequestCreateRequest mapToCreateRequest(Map<String, Object> input) {
                if (input == null)
                        return null;

                return LeaveRequestCreateRequest.builder()
                                .email(String.valueOf(input.getOrDefault("Email", "")))
                                .businessKey(String.valueOf(input.getOrDefault("BusinessKey", "")))
                                .employeeId(input.get("EmployeeID") != null
                                                ? UUID.fromString(input.get("EmployeeID").toString())
                                                : null)

                                // Lưu ý: Camunda trả về String, bạn phải dùng valueOf cho Enum
                                .departmentName(input.get("Department") != null
                                                ? DeparmentNameEnum.valueOf(input.get("Department").toString())
                                                : null)

                                .leaveType(input.get("LeaveType") != null
                                                ? LeaveTypeEnum.valueOf(
                                                                input.get("LeaveType") instanceof java.util.List
                                                                                ? ((java.util.List<?>) input
                                                                                                .get("LeaveType"))
                                                                                                .get(0).toString()
                                                                                : input.get("LeaveType").toString()
                                                                                                .replaceAll("[\\[\\]]",
                                                                                                                "")
                                                                                                .split(",")[0].trim())
                                                : null)

                                // Với Date, cần parse từ String (ISO-8601) nếu Camunda gửi về dạng chuỗi
                                .startDate(input.get("StartDate") != null
                                                ? LocalDate.parse(input.get("StartDate").toString()).atStartOfDay()
                                                : null)

                                .endDate(input.get("EndDate") != null
                                                ? LocalDate.parse(input.get("EndDate").toString()).atStartOfDay()
                                                : null)

                                .fullName(String.valueOf(input.getOrDefault("FullName", "")))
                                .reason(String.valueOf(input.getOrDefault("Reason", "")))

                                .status(input.get("Status") != null
                                                ? LeaveRequestStatusEnum.valueOf(input.get("Status").toString().trim().toUpperCase())
                                                : null)

                                .totalWorkingDays(input.get("TotalWorkingDays") != null
                                                && !input.get("TotalWorkingDays").toString().isEmpty()
                                                                ? new BigDecimal(input.get("TotalWorkingDays")
                                                                                .toString())
                                                                : BigDecimal.ZERO)

                                // Mặc định leaveSession là null hoặc lấy từ map nếu có
                                .leaveSession(input.get("leaveSession") != null
                                                ? LeaveSessionEnum.valueOf(input.get("leaveSession").toString())
                                                : null)
                                .build();
        }

        @Builder
        public record LeaveRequestCreateRequest(
                        @NotBlank(message = "Business Key không được để trống") String businessKey,
                        @NotBlank(message = "Email không được để trống") String email,
                        @NotNull(message = "ID Nhân viên không được để trống") UUID employeeId,
                        @NotNull(message = "Department hông được để trống ") DeparmentNameEnum departmentName,
                        @NotNull(message = "ID Loại nghỉ không được để trống") LeaveTypeEnum leaveType,

                        @NotNull(message = "Ca nghỉ không được để trống") LeaveSessionEnum leaveSession,

                        @NotNull(message = "Ngày bắt đầu không được để trống") LocalDateTime startDate,

                        @NotNull(message = "Ngày kết thúc không được để trống") LocalDateTime endDate,

                        @NotNull(message = "Tổng số ngày làm việc không được để trống") BigDecimal totalWorkingDays,

                        @NotBlank(message = "Tên nhân viên không được để trống") String fullName,

                        String reason,

                        LeaveRequestStatusEnum status,

                        String attachmentUrl) {
        }

        public record LeaveRequestUpdateRequest(
                        @NotBlank(message = "Business Key không được để trống") String businessKey,

                        @NotNull(message = "ID Nhân viên không được để trống") UUID employeeId,

                        UUID currentAssigneeId,

                        String currentAssigneeName,
                        @NotNull(message = "Department hông được để trống ") DeparmentNameEnum departmentName,
                        @NotNull(message = "ID Loại nghỉ không được để trống") LeaveTypeEnum leaveTypeId,

                        @NotNull(message = "Ngày bắt đầu không được để trống") LocalDateTime startDate,

                        @NotNull(message = "Ngày kết thúc không được để trống") LocalDateTime endDate,

                        @NotNull(message = "Tổng số ngày làm việc không được để trống") BigDecimal totalWorkingDays,

                        String reason,

                        LeaveRequestStatusEnum status,

                        String attachmentUrl) {
        }
}
