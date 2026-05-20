package com.example.demo.leavecore.delivery.Handler;

import com.example.demo.common.Dto.BaseResponse;
import com.example.demo.leavecore.delivery.Dto.tasklist.FilterSummaryDto;
import com.example.demo.leavecore.delivery.Dto.tasklist.TaskItemDto;
import com.example.demo.leavecore.service.CamundaTasklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tasklist")
@CrossOrigin(origins = "*")
public class TaskListApiController {

    private final CamundaTasklistService camundaTasklistService;

    @GetMapping("/filters-summary")
    public BaseResponse<FilterSummaryDto> getFiltersSummary(@AuthenticationPrincipal Jwt jwt) {
        String username = getUsernameFromJwt(jwt);
        log.info("--- [API] Fetching filters summary for authenticated user: {} ---", username);
        try {
            List<Map<String, Object>> allOpenTasks = filterTasksByAuth(camundaTasklistService.searchTasks("CREATED", null, null), jwt, username);
            List<Map<String, Object>> assignedTasks = filterTasksByAuth(camundaTasklistService.searchTasks("CREATED", true, username), jwt, username);
            List<Map<String, Object>> unassignedTasks = filterTasksByAuth(camundaTasklistService.searchTasks("CREATED", false, null), jwt, username);
            List<Map<String, Object>> completedTasks = filterTasksByAuth(camundaTasklistService.searchTasks("COMPLETED", null, null), jwt, username);

            FilterSummaryDto summary = new FilterSummaryDto(allOpenTasks.size(), assignedTasks.size(), unassignedTasks.size(), completedTasks.size());
            return BaseResponse.<FilterSummaryDto>builder()
                    .code("200")
                    .message("Thành công")
                    .data(summary)
                    .build();
        } catch (Exception e) {
            log.error("Error fetching filter summary: {}", e.getMessage(), e);
            return BaseResponse.<FilterSummaryDto>builder()
                    .code("500")
                    .message("Lỗi: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/tasks")
    public BaseResponse<List<TaskItemDto>> getTasks(
            @RequestParam(value = "filter", defaultValue = "ALL_OPEN") String filter,
            @AuthenticationPrincipal Jwt jwt) {
        String username = getUsernameFromJwt(jwt);
        if (username == null) {
            return BaseResponse.<List<TaskItemDto>>builder()
                    .code("401")
                    .message("Unauthorized")
                    .build();
        }
        log.info("--- [API] Fetching tasks for filter: {} and user: {} ---", filter, username);
        try {
            List<Map<String, Object>> rawTasks;
            switch (filter.toUpperCase()) {
                case "ASSIGNED_TO_ME":
                    rawTasks = camundaTasklistService.searchTasks("CREATED", true, username);
                    break;
                case "UNASSIGNED":
                    rawTasks = camundaTasklistService.searchTasks("CREATED", false, null);
                    break;
                case "COMPLETED":
                    rawTasks = camundaTasklistService.searchTasks("COMPLETED", null, null);
                    break;
                case "ALL_OPEN":
                default:
                    rawTasks = camundaTasklistService.searchTasks("CREATED", null, null);
                    break;
            }

            // Lọc danh sách task theo quyền hạn group của user
            rawTasks = filterTasksByAuth(rawTasks, jwt, username);

            List<TaskItemDto> tasks = new ArrayList<>();
            for (Map<String, Object> t : rawTasks) {
                String id = (String) t.get("id");
                String name = (String) t.get("name");
                String processId = (String) t.get("processName");
                if (processId == null)
                    processId = (String) t.get("processDefinitionKey");
                String processInstanceKey = (String) t.get("processInstanceKey");
                String processDefinitionKey = (String) t.get("processDefinitionKey");
                String creationTime = (String) t.get("creationTime");
                String assignee = (String) t.get("assignee");
                String taskState = (String) t.get("taskState");
                String formId = (String) t.get("formId");

                // Fetch variables for each task
                Map<String, Object> variables = camundaTasklistService.getTaskVariables(id);

                tasks.add(new TaskItemDto(id, name, processId, processInstanceKey, processDefinitionKey, creationTime,
                        assignee, taskState, formId, variables));
            }

            return BaseResponse.<List<TaskItemDto>>builder()
                    .code("200")
                    .message("Thành công")
                    .data(tasks)
                    .build();
        } catch (Exception e) {
            log.error("Error fetching tasks: {}", e.getMessage(), e);
            return BaseResponse.<List<TaskItemDto>>builder()
                    .code("500")
                    .message("Lỗi: " + e.getMessage())
                    .build();
        }
    }

    private List<Map<String, Object>> filterTasksByAuth(List<Map<String, Object>> rawTasks, Jwt jwt, String username) {
        if (rawTasks == null) {
            return new ArrayList<>();
        }
        if (jwt == null) {
            return rawTasks;
        }

        // In các claims của JWT để phục vụ debug
        log.info("JWT Claims for user {}: {}", username, jwt.getClaims());
        log.info("Raw tasks fetched from Camunda count: {}", rawTasks.size());
        for (int i = 0; i < rawTasks.size(); i++) {
            log.info("Raw Task [{}]: {}", i, rawTasks.get(i));
        }

        // Trích xuất các group của user từ token JWT của Keycloak
        List<String> userGroups = new ArrayList<>();
        Object groupsObj = jwt.getClaim("groups");
        if (groupsObj instanceof List<?>) {
            for (Object item : (List<?>) groupsObj) {
                if (item != null) {
                    userGroups.add(item.toString());
                }
            }
        } else if (groupsObj instanceof String[]) {
            for (String item : (String[]) groupsObj) {
                if (item != null) {
                    userGroups.add(item);
                }
            }
        } else {
            try {
                List<String> list = jwt.getClaimAsStringList("groups");
                if (list != null) {
                    userGroups.addAll(list);
                }
            } catch (Exception ignored) {}
        }

        List<String> finalUserGroups = userGroups.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(g -> g.startsWith("/") ? g.substring(1) : g) // Chuẩn hóa bỏ dấu gạch chéo đầu dòng từ Keycloak
                .map(String::toUpperCase)
                .toList();
        log.info("Parsed and normalized user groups for visibility check: {}", finalUserGroups);

        return rawTasks.stream()
                .filter(t -> {
                    // Nếu task đã được gán cho chính user đang đăng nhập, luôn cho phép xem
                    String assignee = (String) t.get("assignee");
                    if (username != null && username.equals(assignee)) {
                        return true;
                    }

                    // Kiểm tra thuộc tính candidateGroups của task
                    Object candidateGroupsObj = t.get("candidateGroups");
                    if (candidateGroupsObj == null) {
                        return true; // Không tìm thấy thông tin candidateGroups, mặc định cho phép xem
                    }

                    List<String> taskGroups = new ArrayList<>();
                    if (candidateGroupsObj instanceof List<?>) {
                        for (Object obj : (List<?>) candidateGroupsObj) {
                            if (obj != null) {
                                String groupStr = obj.toString().trim();
                                if (!groupStr.isEmpty()) {
                                    // Tách dấu phẩy đề phòng trường hợp chuỗi chứa dạng "HR1, HR2" trong danh sách
                                    String[] parts = groupStr.split(",");
                                    for (String part : parts) {
                                        String normalized = part.trim();
                                        if (normalized.startsWith("/")) {
                                            normalized = normalized.substring(1);
                                        }
                                        taskGroups.add(normalized.toUpperCase());
                                    }
                                }
                            }
                        }
                    } else {
                        String groupStr = candidateGroupsObj.toString().trim();
                        if (!groupStr.isEmpty()) {
                            String[] parts = groupStr.split(",");
                            for (String part : parts) {
                                String normalized = part.trim();
                                if (normalized.startsWith("/")) {
                                    normalized = normalized.substring(1);
                                }
                                taskGroups.add(normalized.toUpperCase());
                            }
                        }
                    }

                    if (taskGroups.isEmpty()) {
                        return true; // Không có giới hạn nhóm, mặc định cho phép xem
                    }

                    // Kiểm tra xem user có thuộc ít nhất một nhóm trong taskGroups không
                    for (String tg : taskGroups) {
                        if (finalUserGroups.contains(tg)) {
                            return true;
                        }
                    }

                    return false; // Task có nhóm ứng viên nhưng user không thuộc nhóm nào
                })
                .toList();
    }

    private String getUsernameFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String username = jwt.getClaimAsString("email");
        if (username == null) {
            username = jwt.getClaimAsString("Email");
        }
        if (username == null) {
            username = jwt.getClaimAsString("preferred_username");
        }
        if (username == null) {
            username = jwt.getSubject();
        }
        return username;
    }
}