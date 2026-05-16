package com.example.demo.leaveinfrastructure.delivery.Dto.ApprovalHistory;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ApprovalHistoryRequest {
    public record ApprovalHistoryCreateRequest(
        @NotNull(message = "Leave Request ID không được để trống")
        UUID leaveRequestId,

        @NotNull(message = "Approver ID không được để trống")
        UUID approverId,

        @NotNull(message = "Level không được để trống")
        Integer level,

        @NotBlank(message = "Action không được để trống")
        String action,

        String comment
    ) {}

    public record ApprovalHistoryUpdateRequest(
        @NotNull(message = "Leave Request ID không được để trống")
        UUID leaveRequestId,

        @NotNull(message = "Approver ID không được để trống")
        UUID approverId,

        @NotNull(message = "Level không được để trống")
        Integer level,

        @NotBlank(message = "Action không được để trống")
        String action,

        String comment
    ) {}
}
