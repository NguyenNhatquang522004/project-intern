package com.example.demo.leavecore.service;

import org.springframework.stereotype.Service;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveRequestRequest.LeaveRequestCreateRequest;
import com.example.demo.leavecore.delivery.Dto.LeaveRequest.LeaveValidRespones;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.ValidateRequestInput;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.ValidateRequestOutput;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveValidateRequestUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ValidateRequestService {

    private final ILeaveValidateRequestUseCase leaveValidateRequestUseCase;

    public ValidateRequestOutput execute(ValidateRequestInput input) {
        log.info("--- [SERVICE] Bắt đầu xử lý ValidateRequest: businessKey={} ---", input.businessKey());

        LeaveRequestCreateRequest data = LeaveRequestCreateRequest.builder()
                .email(input.email())
                .businessKey(input.businessKey())
                .employeeId(input.employeeId())
                .departmentName(input.departmentName())
                .leaveType(input.leaveType())
                .startDate(input.startDate())
                .endDate(input.endDate())
                .fullName(input.fullName())
                .reason(input.reason())
                .status(input.status())
                .totalWorkingDays(input.totalWorkingDays())
                .leaveSession(input.leaveSession())
                .attachmentUrl(input.attachmentUrl())
                .build();

        log.info("Calling leaveValidateRequestUseCase.validate with data: {}", data);
        BaseResponse<LeaveValidRespones> response = leaveValidateRequestUseCase.validate(data);
        log.info("Received usecase response: {}", response);

        LeaveValidRespones validData = response.getData();
        if (validData == null) {
            log.error("Validation response data is null!");
            throw new RuntimeException("Validation response data is null!");
        }

        ValidateRequestOutput output = ValidateRequestOutput.builder()
                .isValid(validData.getIsvalid())
                .totalWorkingDays(validData.getTotalWorkingDays())
                .businessKey(validData.getBusinessKey())
                .message(validData.getMessage() != null ? validData.getMessage() : response.getMessage())
                .error(response.getCode())
                .build();

        log.info("--- [SERVICE] Kết thúc xử lý ValidateRequest: isValid={} ---", output.isValid());
        return output;
    }
}
