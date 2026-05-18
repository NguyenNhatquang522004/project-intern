package com.example.demo.leavecore.service;

import org.springframework.stereotype.Service;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestResponse;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.DeductBalanceInput;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.DeductBalanceOutput;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveDeductBalanceUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeductBalanceService {

    private final ILeaveDeductBalanceUseCase leaveDeductBalanceUseCase;

    public DeductBalanceOutput execute(DeductBalanceInput input) {
        log.info("--- [SERVICE] Bắt đầu xử lý DeductBalance: businessKey={} ---", input.businessKey());

        BaseResponse<LeaveRequestResponse> response = leaveDeductBalanceUseCase.deductBalance(input.businessKey());
        log.info("Received response from leaveDeductBalanceUseCase: {}", response);

        if (!"200".equals(response.getCode())) {
            log.error("Deduct balance failed. Code: {}, Message: {}", response.getCode(), response.getMessage());
            throw new RuntimeException(response.getMessage());
        }

        log.info("--- [SERVICE] Kết thúc xử lý DeductBalance: Thành công ---");
        return new DeductBalanceOutput("Leave request approved and balance deducted successfully", true);
    }
}
