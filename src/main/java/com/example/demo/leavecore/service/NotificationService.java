package com.example.demo.leavecore.service;

import org.springframework.stereotype.Service;

import com.example.demo.leavecore.delivery.Dto.ManualTrigger.NotificationInput;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.NotificationOutput;
import com.example.demo.leavecore.usecase.IUseCase.ILeaveNotificationUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final ILeaveNotificationUseCase leaveNotificationUseCase;

    public NotificationOutput execute(NotificationInput input) {
        log.info("--- [SERVICE] Bắt đầu xử lý Notification: email={}, status={}, totalWorkingDays={} ---", 
                 input.email(), input.status(), input.totalWorkingDays());

        try {
            leaveNotificationUseCase.sendLeaveStatusUpdateEmail(
                    input.email(),
                    input.status(),
                    input.totalWorkingDays(),
                    input.message() != null ? input.message() : ""
            );
            log.info("Successfully sent leave status update email.");
            return new NotificationOutput(true, "Notification email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send notification email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification email: " + e.getMessage());
        }
    }
}
