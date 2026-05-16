package com.example.demo.leavecore.usecase.adapterUseCase;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestResponse;
import com.example.demo.leavecore.domain.IRepository.IRepositoryLeaveRequest;
import com.example.demo.leavecore.domain.entity.LeaveRequest;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveUpdateStatusUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveUpdateStatusUseCase implements ILeaveUpdateStatusUseCase {
    private final IRepositoryLeaveRequest repositoryLeaveRequest;

    @Override
    public BaseResponse<LeaveRequestResponse> updateStatus(String businessKey, LeaveRequestStatusEnum status) {
        try {
            Optional<LeaveRequest> leaveRequest = repositoryLeaveRequest.findByBusinessKey(businessKey);
            if (leaveRequest.isEmpty()) {
                return BaseResponse.<LeaveRequestResponse>builder().code("404").message("Leave request not found")
                        .build();
            }
            LeaveRequest leaveRequestEntity = leaveRequest.get();
            if (status == LeaveRequestStatusEnum.APPROVED) {
                leaveRequestEntity.setStatus(status);
            } else if (status == LeaveRequestStatusEnum.REJECTED) {
                leaveRequestEntity.setStatus(status);
            }
            repositoryLeaveRequest.save(leaveRequestEntity);
            return BaseResponse.<LeaveRequestResponse>builder().code("200")
                    .message("Leave request updated successfully")
                    .build();
        } catch (Exception e) {
            return BaseResponse.<LeaveRequestResponse>builder().code("500").message("Internal server error")
                    .build();
        }
    }
}
