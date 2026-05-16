package com.example.demo.leavecore.usecase.IUseCase;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveValidRespones;

public interface ILeaveValidateRequestUseCase {
    BaseResponse<LeaveValidRespones> validate(LeaveRequestCreateRequest request);
}
