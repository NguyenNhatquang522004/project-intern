package com.example.demo.leavecore.delivery.Handler;

import com.example.demo.leavecore.delivery.Dto.manualtask.TaskAssignRequestDto;
import com.example.demo.leavecore.delivery.Dto.manualtask.TaskCompleteRequestDto;
import com.example.demo.leavecore.delivery.Dto.manualtask.UserTaskResponseDto;
import com.example.demo.leavecore.service.UserTaskTriggerService;
import io.camunda.zeebe.client.api.command.ClientException;
import io.camunda.zeebe.client.api.command.ClientStatusException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller tiếp nhận yêu cầu từ UI để trực tiếp điều khiển vòng đời User Task (Assign & Complete) trong Camunda 8.
 * Từ đó, gián tiếp kích hoạt các Task Listener tương ứng (task-assign và task-complete) để thực thi nghiệp vụ tự động.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/manual-tasks")
public class UserTaskTriggerController {

    private final UserTaskTriggerService userTaskTriggerService;

    /**
     * API Gán task (Assign Task)
     * POST /api/v1/manual-tasks/{taskId}/assign
     * 
     * @param taskId ID của User Task (Task Key)
     * @param requestDto Thông tin người gán
     * @return UserTaskResponseDto dạng JSON chuẩn hóa
     */
    @PostMapping("/{taskId}/assign")
    public ResponseEntity<UserTaskResponseDto> assignTask(
            @PathVariable("taskId") long taskId,
            @RequestBody @Valid TaskAssignRequestDto requestDto) {
        log.info("--- [API TRIGGER] Yêu cầu gán User Task từ UI: taskId={}, assignee={} ---", taskId, requestDto.assignee());

        try {
            userTaskTriggerService.assignTask(taskId, requestDto.assignee());
            log.info("--- [API TRIGGER] Gán User Task thành công: taskId={} ---", taskId);
            return ResponseEntity.ok(UserTaskResponseDto.success("Đã gán task thành công trên hệ thống Camunda."));
        } catch (ClientStatusException e) {
            log.error("--- [API TRIGGER] Lỗi gRPC từ Camunda Broker khi gán task {}: {} ---", taskId, e.getMessage(), e);
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            String msg = "Gặp lỗi từ dịch vụ điều phối Camunda khi thực hiện gán task.";
            if (e.getStatusCode() == io.grpc.Status.Code.UNAVAILABLE 
                    || e.getStatusCode() == io.grpc.Status.Code.DEADLINE_EXCEEDED) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                msg = "Kết nối đến Camunda Cloud Broker bị gián đoạn. Vui lòng kiểm tra lại cấu hình hoặc kết nối mạng.";
            } else if (e.getStatusCode() == io.grpc.Status.Code.NOT_FOUND) {
                status = HttpStatus.NOT_FOUND;
                msg = "Không tìm thấy User Task có ID tương ứng trên hệ thống Camunda.";
            }
            return ResponseEntity.status(status)
                    .body(UserTaskResponseDto.error(msg + " Chi tiết: " + e.getMessage()));
        } catch (ClientException e) {
            log.error("--- [API TRIGGER] Lỗi kết nối Zeebe Client khi gán task {}: {} ---", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(UserTaskResponseDto.error("Không thể kết nối đến Camunda Cloud Broker. Chi tiết: " + e.getMessage()));
        } catch (Exception e) {
            log.error("--- [API TRIGGER] Lỗi hệ thống nội bộ khi gán task {}: {} ---", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UserTaskResponseDto.error("Lỗi hệ thống nội bộ khi gán User Task. Chi tiết: " + e.getMessage()));
        }
    }

    /**
     * API Hoàn thành task (Complete Task)
     * POST /api/v1/manual-tasks/{taskId}/complete
     * 
     * @param taskId ID của User Task (Task Key)
     * @param requestDto Trạng thái duyệt (APPROVED/REJECTED) và assignee
     * @return UserTaskResponseDto dạng JSON chuẩn hóa
     */
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<UserTaskResponseDto> completeTask(
            @PathVariable("taskId") long taskId,
            @RequestBody @Valid TaskCompleteRequestDto requestDto) {
        log.info("--- [API TRIGGER] Yêu cầu hoàn thành User Task từ UI: taskId={}, status={}, assignee={} ---", 
                taskId, requestDto.status(), requestDto.assignee());

        try {
            userTaskTriggerService.completeTask(taskId, requestDto.status(), requestDto.assignee());
            log.info("--- [API TRIGGER] Hoàn thành User Task thành công: taskId={} ---", taskId);
            return ResponseEntity.ok(UserTaskResponseDto.success("Đã phê duyệt/hoàn thành task thành công trên hệ thống Camunda."));
        } catch (ClientStatusException e) {
            log.error("--- [API TRIGGER] Lỗi gRPC từ Camunda Broker khi hoàn thành task {}: {} ---", taskId, e.getMessage(), e);
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            String msg = "Gặp lỗi từ dịch vụ điều phối Camunda khi thực hiện hoàn thành task.";
            if (e.getStatusCode() == io.grpc.Status.Code.UNAVAILABLE 
                    || e.getStatusCode() == io.grpc.Status.Code.DEADLINE_EXCEEDED) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                msg = "Kết nối đến Camunda Cloud Broker bị gián đoạn. Vui lòng kiểm tra lại cấu hình hoặc kết nối mạng.";
            } else if (e.getStatusCode() == io.grpc.Status.Code.NOT_FOUND) {
                status = HttpStatus.NOT_FOUND;
                msg = "Không tìm thấy User Task có ID tương ứng trên hệ thống Camunda.";
            }
            return ResponseEntity.status(status)
                    .body(UserTaskResponseDto.error(msg + " Chi tiết: " + e.getMessage()));
        } catch (ClientException e) {
            log.error("--- [API TRIGGER] Lỗi kết nối Zeebe Client khi hoàn thành task {}: {} ---", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(UserTaskResponseDto.error("Không thể kết nối đến Camunda Cloud Broker. Chi tiết: " + e.getMessage()));
        } catch (Exception e) {
            log.error("--- [API TRIGGER] Lỗi hệ thống nội bộ khi hoàn thành task {}: {} ---", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(UserTaskResponseDto.error("Lỗi hệ thống nội bộ khi hoàn thành User Task. Chi tiết: " + e.getMessage()));
        }
    }
}
