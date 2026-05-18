package com.example.demo.leavecore.delivery.Handler;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.service.CamundaTasklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tasks")
@CrossOrigin(origins = "*")
public class UserTaskHandler {

    private final CamundaTasklistService camundaTasklistService;

    @PostMapping("/{taskId}/assign")
    public BaseResponse<Void> assignTask(
            @PathVariable("taskId") String taskId, 
            @AuthenticationPrincipal Jwt jwt) {
        String username = getUsernameFromJwt(jwt);
        log.info("--- [API] Assigning task {} automatically to authenticated user: {} ---", taskId, username);
        try {
            camundaTasklistService.assignTask(taskId, username);
            return BaseResponse.<Void>builder()
                    .code("200")
                    .message("Gán task thành công")
                    .build();
        } catch (Exception e) {
            log.error("Error assigning task: {}", e.getMessage(), e);
            return BaseResponse.<Void>builder()
                    .code("500")
                    .message("Thất bại: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/{taskId}/unassign")
    public BaseResponse<Void> unassignTask(@PathVariable("taskId") String taskId) {
        log.info("--- [API] Unassigning task {} ---", taskId);
        try {
            camundaTasklistService.assignTask(taskId, null);
            return BaseResponse.<Void>builder()
                    .code("200")
                    .message("Giải phóng task thành công")
                    .build();
        } catch (Exception e) {
            log.error("Error unassigning task: {}", e.getMessage(), e);
            return BaseResponse.<Void>builder()
                    .code("500")
                    .message("Thất bại: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/{taskId}/complete")
    public BaseResponse<Void> completeTask(
            @PathVariable("taskId") String taskId, 
            @RequestParam("decision") String decision) {
        log.info("--- [API] Completing task {} with decision {} ---", taskId, decision);
        try {
            camundaTasklistService.completeTask(taskId, decision);
            return BaseResponse.<Void>builder()
                    .code("200")
                    .message("Hoàn thành task thành công")
                    .build();
        } catch (Exception e) {
            log.error("Error completing task: {}", e.getMessage(), e);
            return BaseResponse.<Void>builder()
                    .code("500")
                    .message("Thất bại: " + e.getMessage())
                    .build();
        }
    }

    private String getUsernameFromJwt(Jwt jwt) {
        if (jwt == null) {
            return "demo_manager@company.com";
        }
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null) {
            username = jwt.getClaimAsString("email");
        }
        return username != null ? username : "demo_manager@company.com";
    }
}
