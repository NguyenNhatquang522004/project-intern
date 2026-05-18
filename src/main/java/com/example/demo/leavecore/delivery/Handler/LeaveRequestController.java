package com.example.demo.leavecore.delivery.Handler;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.trigger.LeaveRequestDto;
import com.example.demo.leavecore.delivery.Dto.trigger.LeaveRequestResponseDto;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.ClientException;
import io.camunda.zeebe.client.api.command.ClientStatusException;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller tiếp nhận yêu cầu xin nghỉ phép từ giao diện người dùng và kích hoạt quy trình tự động hóa.
 * Áp dụng Resilient Exception Handling để bảo vệ hệ thống trước các lỗi gRPC/giao tiếp mạng với Camunda Cloud.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController {

    private final ZeebeClient zeebeClient;

    /**
     * Tiếp nhận đơn nghỉ phép từ giao diện UI và kích hoạt một tiến trình mới (Process Instance) trên Camunda.
     * 
     * @param requestDto dữ liệu đơn nghỉ phép đã qua kiểm tra hợp lệ đầu vào (JSR-380)
     * @return phản hồi chuẩn RESTful BaseResponse chứa mã ProcessInstanceKey
     */
    @PostMapping("/submit")
    public ResponseEntity<BaseResponse<LeaveRequestResponseDto>> submitLeaveRequest(
            @RequestBody @Valid LeaveRequestDto requestDto) {
        log.info("--- [API] Tiếp nhận đơn xin nghỉ phép từ UI của: {} ---", requestDto.getFullName());

        // Đảm bảo trạng thái mặc định của đơn xin nghỉ phép luôn là PENDING
        requestDto.setStatus("PENDING");

        try {
            log.info("--- [API] Bắt đầu kích hoạt quy trình Camunda với Process ID: Process_1oveniu ---");
            
            // Sử dụng newCreateInstanceCommand() chuẩn của Zeebe Client để bắt đầu quy trình mới
            ProcessInstanceEvent instanceEvent = zeebeClient.newCreateInstanceCommand()
                    .bpmnProcessId("Process_ebgtn9t") 
                    .latestVersion()
                    .variables(requestDto) // Tự động serialize DTO sang dạng các biến quy trình trong Zeebe            
                    .send()
                    .join();

            log.info("--- [API] Đã kích hoạt thành công Instance ID: {} ---", instanceEvent.getProcessInstanceKey());

            LeaveRequestResponseDto responseData = new LeaveRequestResponseDto(instanceEvent.getProcessInstanceKey());

            return ResponseEntity.ok(BaseResponse.<LeaveRequestResponseDto>builder()
                    .code("200")
                    .message("Đơn nghỉ phép đã được gửi và đang xử lý tự động!")
                    .data(responseData)
                    .build());
            
        } catch (ClientStatusException e) {
            // Xử lý các lỗi phản hồi gRPC cụ thể từ Camunda Broker
            log.error("--- [API] Lỗi gRPC từ Zeebe Broker: Status Code = {}, Description = {} ---", 
                      e.getStatusCode(), e.getStatus().getDescription(), e);

            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            String message = "Kích hoạt quy trình xin nghỉ phép thất bại do lỗi từ dịch vụ điều phối Camunda.";

            if (e.getStatusCode() == io.grpc.Status.Code.UNAVAILABLE 
                    || e.getStatusCode() == io.grpc.Status.Code.DEADLINE_EXCEEDED) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                message = "Dịch vụ Camunda Cloud Broker hiện không phản hồi hoặc bị gián đoạn kết nối. Vui lòng thử lại sau.";
            }

            return ResponseEntity.status(status)
                    .body(BaseResponse.error(String.valueOf(status.value()), message + " Chi tiết: " + e.getMessage()));

        } catch (ClientException e) {
            // Xử lý các lỗi chung từ phía client kết nối Zeebe (ví dụ: cấu hình sai, lỗi mạng kết nối ban đầu)
            log.error("--- [API] Lỗi kết nối Zeebe Client: {} ---", e.getMessage(), e);
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(BaseResponse.error("503", "Không thể kết nối đến Camunda Cloud Broker. Vui lòng kiểm tra lại cấu hình dịch vụ. Chi tiết: " + e.getMessage()));

        } catch (Exception e) {
            // Bẫy lỗi ngoại lệ chung để bảo vệ API không bị sập và phản hồi thông tin chi tiết
            log.error("--- [API] Lỗi hệ thống không xác định khi kích hoạt quy trình: {} ---", e.getMessage(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("500", "Lỗi hệ thống nội bộ khi xử lý đơn xin nghỉ phép. Chi tiết: " + e.getMessage()));
        }
    }
}
