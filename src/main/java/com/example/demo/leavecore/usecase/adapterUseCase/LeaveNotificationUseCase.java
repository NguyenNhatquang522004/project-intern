package com.example.demo.leavecore.usecase.adapterUseCase;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.common.Dto.EmailRequest;
import com.example.demo.common.share.email.IEmail;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveNotificationUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeaveNotificationUseCase implements ILeaveNotificationUseCase {

    private final IEmail emailAdapter;

    @Override
    public void sendLeaveStatusUpdateEmail(String userId, String status, String lastWorkingDay, String managerNote) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", userId);
        variables.put("applicationDate", LocalDateTime.now());
        variables.put("status", status);
        variables.put("lastWorkingDay", lastWorkingDay != null ? lastWorkingDay : "");
        variables.put("managerNote", managerNote != null ? managerNote : "");
        emailAdapter.sendEmail(EmailRequest.builder()
                .to(userId)
                .subject("Application Status Update")
                .templateName("emails/resignation-status")
                .variables(variables)
                .build());
    }

}
