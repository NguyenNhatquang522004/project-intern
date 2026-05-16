package com.example.demo.leaveinfrastructure.delivery.Dto.ApprovalHistory;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalHistoryResponse {
    private UUID id;
    private UUID leaveRequestId;
    private UUID approverId;
    private Integer level;
    private String action;
    private String comment;
    private LocalDateTime createdAt;
}
