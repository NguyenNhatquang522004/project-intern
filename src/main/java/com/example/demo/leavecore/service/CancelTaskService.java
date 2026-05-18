package com.example.demo.leavecore.service;

import org.springframework.stereotype.Service;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestResponse;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.CancelTaskInput;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.CancelTaskOutput;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveUpdateStatusUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class CancelTaskService {

    private final ILeaveUpdateStatusUseCase leaveUpdateStatusUseCase;

    public CancelTaskOutput execute(CancelTaskInput input) {
        log.info("--- [SERVICE] Bắt đầu xử lý CancelTask: businessKey={} ---", input.businessKey());

        BaseResponse<LeaveRequestResponse> response = leaveUpdateStatusUseCase.updateStatus(
                input.businessKey(),
                LeaveRequestStatusEnum.REJECTED
        );
        log.info("Received response from leaveUpdateStatusUseCase: {}", response);

        if (!"200".equals(response.getCode())) {
            log.error("Cancel task failed to update status to REJECTED. Code: {}, Message: {}", 
                      response.getCode(), response.getMessage());
            throw new RuntimeException(response.getMessage());
        }

        log.info("--- [SERVICE] Kết thúc xử lý CancelTask: Thành công ---");
        return new CancelTaskOutput("Cancel leave request success", true);
    }
}
