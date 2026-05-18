package com.example.demo.leavecore.delivery.Handler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.ManualTrigger.*;
import com.example.demo.leavecore.service.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/manual-trigger")
public class ManualTriggerController {

    private final ValidateRequestService validateRequestService;
    private final DeductBalanceService deductBalanceService;
    private final NotificationService notificationService;
    private final UpdateStatusService updateStatusService;
    private final CancelTaskService cancelTaskService;
    private final TaskAssignmentService taskAssignmentService;
    private final TaskCompleteService taskCompleteService;
    private final EndEventService endEventService;
    private final EndSuccessService endSuccessService;
    private final BeginService beginService;

    @PostMapping("/validate-request")
    public ResponseEntity<BaseResponse<ValidateRequestOutput>> validateRequest(
            @RequestBody @Valid ValidateRequestInput input) {
        log.info("--- [API] Nhận yêu cầu kích hoạt thủ công validate-request: businessKey={} ---", input.businessKey());
        ValidateRequestOutput output = validateRequestService.execute(input);
        return ResponseEntity.ok(BaseResponse.success(output));
    }

    @PostMapping("/deduct-balance")
    public ResponseEntity<BaseResponse<DeductBalanceOutput>> deductBalance(
            @RequestBody @Valid DeductBalanceInput input) {
        log.info("--- [API] Nhận yêu cầu kích hoạt thủ công deduct-balance: businessKey={} ---", input.businessKey());
        DeductBalanceOutput output = deductBalanceService.execute(input);
        return ResponseEntity.ok(BaseResponse.success(output));
    }

    @PostMapping("/notify")
    public ResponseEntity<BaseResponse<NotificationOutput>> notify(
            @RequestBody @Valid NotificationInput input) {
        log.info("--- [API] Nhận yêu cầu kích hoạt thủ công notify: email={} ---", input.email());
        NotificationOutput output = notificationService.execute(input);
        return ResponseEntity.ok(BaseResponse.success(output));
    }

    @PostMapping("/update-status")
    public ResponseEntity<BaseResponse<UpdateStatusOutput>> updateStatus(
            @RequestBody @Valid UpdateStatusInput input) {
        log.info("--- [API] Nhận yêu cầu kích hoạt thủ công update-status: businessKey={}, status={} ---", 
                 input.businessKey(), input.status());
        UpdateStatusOutput output = updateStatusService.execute(input);
        return ResponseEntity.ok(BaseResponse.success(output));
    }

    @PostMapping("/cancel-task")
    public ResponseEntity<BaseResponse<CancelTaskOutput>> cancelTask(
            @RequestBody @Valid CancelTaskInput input) {
        log.info("--- [API] Nhận yêu cầu kích hoạt thủ công cancel-task: businessKey={} ---", input.businessKey());
        CancelTaskOutput output = cancelTaskService.execute(input);
        return ResponseEntity.ok(BaseResponse.success(output));
    }

    @PostMapping("/task-assign")
    public ResponseEntity<BaseResponse<TaskAssignmentOutput>> taskAssign(
            @RequestBody @Valid TaskAssignmentInput input) {
        log.info("--- [API] Nhận yêu cầu kích hoạt thủ công task-assign: businessKey={}, assignee={} ---", 
                 input.businessKey(), input.assignee());
        TaskAssignmentOutput output = taskAssignmentService.execute(input);
        return ResponseEntity.ok(BaseResponse.success(output));
    }

    @PostMapping("/task-complete")
    public ResponseEntity<BaseResponse<TaskCompleteOutput>> taskComplete(
            @RequestBody @Valid TaskCompleteInput input) {
        log.info("--- [API] Nhận yêu cầu kích hoạt thủ công task-complete: businessKey={}, status={} ---", 
                 input.businessKey(), input.status());
        TaskCompleteOutput output = taskCompleteService.execute(input);
        return ResponseEntity.ok(BaseResponse.success(output));
    }

    @PostMapping("/end-event")
    public ResponseEntity<BaseResponse<EndEventOutput>> endEvent(
            @RequestBody @Valid EndEventInput input) {
        log.info("--- [API] Nhận yêu cầu kích hoạt thủ công end-event: businessKey={} ---", input.businessKey());
        EndEventOutput output = endEventService.execute(input);
        return ResponseEntity.ok(BaseResponse.success(output));
    }

    @PostMapping("/end-success")
    public ResponseEntity<BaseResponse<EndSuccessOutput>> endSuccess(
            @RequestBody @Valid EndSuccessInput input) {
        log.info("--- [API] Nhận yêu cầu kích hoạt thủ công end-success: businessKey={} ---", input.businessKey());
        EndSuccessOutput output = endSuccessService.execute(input);
        return ResponseEntity.ok(BaseResponse.success(output));
    }

    @PostMapping("/begin")
    public ResponseEntity<BaseResponse<BeginOutput>> begin(
            @RequestBody @Valid BeginInput input) {
        log.info("--- [API] Nhận yêu cầu kích hoạt thủ công begin ---");
        BeginOutput output = beginService.execute(input);
        return ResponseEntity.ok(BaseResponse.success(output));
    }
}
