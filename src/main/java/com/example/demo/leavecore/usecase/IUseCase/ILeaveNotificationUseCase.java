package com.example.demo.leavecore.usecase.IUseCase;

public interface ILeaveNotificationUseCase {

    void sendLeaveStatusUpdateEmail(String userId, String status, String lastWorkingDay, String managerNote);
}
