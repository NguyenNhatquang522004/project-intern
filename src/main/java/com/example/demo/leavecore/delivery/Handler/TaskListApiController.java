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
            long allOpen = camundaTasklistService.searchTasks("CREATED", null, null).size();
            long assignedToMe = camundaTasklistService.searchTasks("CREATED", true, username).size();
            long unassigned = camundaTasklistService.searchTasks("CREATED", false, null).size();
            long completed = camundaTasklistService.searchTasks("COMPLETED", null, null).size();

            FilterSummaryDto summary = new FilterSummaryDto(allOpen, assignedToMe, unassigned, completed);
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

    private String getUsernameFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;// Fallback an toàn cho test sandbox
        }
        String username = jwt.getClaimAsString("Email");
        if (username == null) {
            return null;
        }
        return username;
    }
}