package com.example.demo.leavecore.usecase.IUseCase;

import com.example.demo.common.Enum.LeaveRequestStatusEnum;

public interface IHandleTaskEventUseCase {
    void handleTaskAssignEvent(String bussinesskey, String emailAssignee);

    void handleTaskCompleteEvent(String bussinesskey, LeaveRequestStatusEnum status);
}
