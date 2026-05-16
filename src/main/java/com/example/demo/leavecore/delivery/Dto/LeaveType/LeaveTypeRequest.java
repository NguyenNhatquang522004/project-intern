package com.example.demo.leavecore.delivery.Dto.LeaveType;

import com.example.demo.common.Enum.LeaveTypeEnum;

import jakarta.validation.constraints.*;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LeaveTypeRequest {
    public record LeaveTypeCreateRequest(
            @NotBlank(message = "Mã không được để trống") String code,

            @NotBlank(message = "Tên không được để trống") String name,

            @NotNull(message = "Trường requiresProof không được để trống") Boolean requiresProof,

            @NotNull(message = "Trường isPaid không được để trống") LeaveTypeEnum isPaid) {
    }

    public record LeaveTypeUpdateRequest(
            @NotBlank(message = "Mã không được để trống") String code,

            @NotBlank(message = "Tên không được để trống") String name,

            @NotNull(message = "Trường requiresProof không được để trống") Boolean requiresProof,

            @NotNull(message = "Trường isPaid không được để trống")
            LeaveTypeEnum isPaid) {
    }
}
