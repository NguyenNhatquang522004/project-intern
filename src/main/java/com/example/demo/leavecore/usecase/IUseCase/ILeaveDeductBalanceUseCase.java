package com.example.demo.leavecore.usecase.IUseCase;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestResponse;

public interface ILeaveDeductBalanceUseCase {
    BaseResponse<LeaveRequestResponse> deductBalance(String businessKey);
}
