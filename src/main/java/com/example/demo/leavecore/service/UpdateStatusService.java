package com.example.demo.leavecore.service;

import org.springframework.stereotype.Service;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestResponse;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.UpdateStatusInput;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.UpdateStatusOutput;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveUpdateStatusUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class UpdateStatusService {

    private final ILeaveUpdateStatusUseCase leaveUpdateStatusUseCase;

    public UpdateStatusOutput execute(UpdateStatusInput input) {
        log.info("--- [SERVICE] Bắt đầu xử lý UpdateStatus: businessKey={}, status={} ---", input.businessKey(), input.status());

        BaseResponse<LeaveRequestResponse> response = leaveUpdateStatusUseCase.updateStatus(input.businessKey(), input.status());
        log.info("Received response from leaveUpdateStatusUseCase: {}", response);

        if (!"200".equals(response.getCode())) {
            log.error("Update status failed. Code: {}, Message: {}", response.getCode(), response.getMessage());
            throw new RuntimeException(response.getMessage());
        }

        log.info("--- [SERVICE] Kết thúc xử lý UpdateStatus: Thành công ---");
        return new UpdateStatusOutput(input.status(), true, true, "Update status success");
    }
}
