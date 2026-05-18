package com.example.demo.leavecore.delivery.Dto.trigger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin trả về sau khi kích hoạt thành công một Process Instance trên Camunda.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponseDto {
    private Long processInstanceKey;
}
