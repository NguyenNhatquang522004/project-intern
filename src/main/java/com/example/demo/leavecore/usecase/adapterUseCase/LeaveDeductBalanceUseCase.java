package com.example.demo.leavecore.usecase.adapterUseCase;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.common.Enum.LeaveRequestStatusEnum;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestResponse;
import com.example.demo.leavecore.domain.IRepository.IRepositoryLeaveBalance;
import com.example.demo.leavecore.domain.IRepository.IRepositoryLeaveRequest;
import com.example.demo.leavecore.domain.entity.LeaveBalance;
import com.example.demo.leavecore.domain.entity.LeaveRequest;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveDeductBalanceUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveDeductBalanceUseCase implements ILeaveDeductBalanceUseCase {
    private final IRepositoryLeaveRequest repositoryLeaveRequest;
    private final IRepositoryLeaveBalance repositoryLeaveBalance;

    @Override
    public BaseResponse<LeaveRequestResponse> deductBalance(String businessKey) {
        try {
            Optional<LeaveRequest> leaveRequest = repositoryLeaveRequest.findByBusinessKey(businessKey);
            if (leaveRequest.isEmpty()) {
                return BaseResponse.<LeaveRequestResponse>builder().code("404").message("Leave request not found")
                        .build();
            }
            LeaveRequest leaveRequestEntity = leaveRequest.get();
            leaveRequestEntity.setStatus(LeaveRequestStatusEnum.APPROVED);
            repositoryLeaveRequest.save(leaveRequestEntity);
            List<LeaveBalance> leaveBalances = repositoryLeaveBalance.findByEmployee_Id(
                    leaveRequestEntity.getEmployee().getId());
            if (leaveBalances.isEmpty() || leaveBalances.size() > 1) {
                return BaseResponse.<LeaveRequestResponse>builder().code("400").message("Leave balance not found")
                        .build();
            }
            LeaveBalance leaveBalance = leaveBalances.get(0);
            leaveBalance.setUsedDays(leaveBalance.getUsedDays().add(leaveRequestEntity.getTotalWorkingDays()));
            leaveBalance
                    .setPendingDays(leaveBalance.getPendingDays().subtract(leaveRequestEntity.getTotalWorkingDays()));
            repositoryLeaveBalance.save(leaveBalance);

            return BaseResponse.<LeaveRequestResponse>builder().code("200")
                    .message("Leave request updated successfully")
                    .build();
        } catch (Exception e) {
            return BaseResponse.<LeaveRequestResponse>builder().code("500").message("Internal server error")
                    .build();
        }
    }
}
