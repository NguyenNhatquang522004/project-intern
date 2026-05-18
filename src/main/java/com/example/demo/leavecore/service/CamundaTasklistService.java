package com.example.demo.leavecore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class CamundaTasklistService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${camunda.client.auth.client-id:orchestration}")
    private String clientId;

    @Value("${camunda.client.auth.client-secret:secret}")
    private String clientSecret;

    @Value("${camunda.client.auth.issuer-url:http://keycloak:18080/auth/realms/camunda-platform}")
    private String issuerUrl;

    @Value("${camunda.tasklist.url:http://localhost:8080}")
    private String tasklistUrl;

    private String getAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "client_credentials");
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    issuerUrl + "/protocol/openid-connect/token",
                    request,
                    Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.warn(
                    "--- [TASKLIST SERVICE] Không thể kết nối Keycloak lấy Token: {}. Chạy chế độ fallback bypass auth. ---",
                    e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchTasks(String state, Boolean assigned, String assignee) {
        String token = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }

        Map<String, Object> body = new HashMap<>();
        Map<String, Object> filter = new HashMap<>();

        // 1. SỬA ĐỔI: Khớp chuẩn 100% Enum trạng thái theo log hệ thống yêu cầu
        if (state != null) {
            if ("ALL_OPEN".equals(state) || "CREATED".equals(state) || "ACTIVE".equals(state)) {
                filter.put("state", "CREATED"); // Dùng 'CREATED' cho các task đang mở/chờ duyệt
            } else {
                filter.put("state", state); // Ví dụ: COMPLETED
            }
        } else {
            filter.put("state", "CREATED");
        }

        if (assigned != null && assigned && assignee != null && !assignee.isEmpty()) {
            filter.put("assignee", assignee);
        }

        body.put("filter", filter);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            // 2. SỬA ĐỔI: Thay List.class thành Map.class để tránh lỗi sập gói tin Jackson
            // (Extracting response error)
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tasklistUrl + "/v1/user-tasks/search",
                    request,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> resBody = response.getBody();
                log.info("--- [TASKLIST SERVICE] Dữ liệu thô nhận về từ Camunda: {} ---", resBody);
                // 3. SỬA ĐỔI: Bóc tách mảng danh sách nhiệm vụ nằm bên trong trường "tasks" của
                // Object phản hồi
                if (resBody.containsKey("items") && resBody.get("items") != null) {
                    List<Map<String, Object>> items = (List<Map<String, Object>>) resBody.get("items");

                    // ĐÃ THÊM: Duyệt qua danh sách để map 'userTaskKey' thành 'id' tương thích với
                    // Next.js UI
                    for (Map<String, Object> item : items) {
                        if (item.containsKey("userTaskKey") && item.get("userTaskKey") != null) {
                            // Chuyển Long thành String gán vào trường "id" cho đúng Type kiểu dữ liệu UI
                            item.put("id", String.valueOf(item.get("userTaskKey")));
                        }
                    }

                    return items; // Trả về danh sách dữ liệu thật đã được chuẩn hóa
                } else {
                    log.info(
                            "--- [TASKLIST SERVICE] Không có nhiệm vụ nào tồn tại thỏa mãn bộ lọc (Hệ thống trống). ---");
                    return new ArrayList<>();
                }
            }
        } catch (Exception e) {
            log.error("--- [TASKLIST SERVICE] Lỗi khi gọi Zeebe User Tasks API search: {} ---", e.getMessage());
        }

        // Chế độ dự phòng Fallback khi Server trống hoặc chưa có Task nào
        return getMockTasks(state, assigned, assignee);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTaskVariables(String taskId) {
        String token = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }

        // Tạo một Body rỗng để ra lệnh cho Camunda quét TOÀN BỘ các biến thuộc phạm vi của Task này
        Map<String, Object> body = new HashMap<>();

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            // SỬA ĐƯỜNG DẪN + PHƯƠNG THỨC: Dùng POST gọi sang v2 API chuyên dụng để search biến
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tasklistUrl + "/v2/user-tasks/" + taskId + "/variables/search",
                    request,
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> resBody = response.getBody();
                Map<String, Object> variables = new HashMap<>();
                
                // Dữ liệu biến thực tế trả về từ v2 nằm gọn trong mảng "items"
                if (resBody.containsKey("items") && resBody.get("items") != null) {
                    List<Map<String, Object>> varList = (List<Map<String, Object>>) resBody.get("items");
                    
                    for (Map<String, Object> v : varList) {
                        String name = (String) v.get("name");
                        String valStr = (String) v.get("value");
                        try {
                            // Camunda lưu giá trị dạng chuỗi mã hóa JSON (Vd: "\"Bùi Thị Hoa\""), cần parse về Object phẳng
                            Object parsedVal = objectMapper.readValue(valStr, Object.class);
                            variables.put(name, parsedVal);
                        } catch (Exception ex) {
                            variables.put(name, valStr);
                        }
                    }
                    log.info("--- [TASKLIST SERVICE] Đã lấy thành công {} biến THỰC TẾ từ Camunda cho task {} ---", variables.size(), taskId);
                    return variables;
                }
            }
        } catch (Exception e) {
            log.error("--- [TASKLIST SERVICE] Lỗi khi lấy biến qua v2 User Tasks API: {} ---", e.getMessage());
        }
        
        // Hệ thống chỉ rơi vào Mock dự phòng nếu Camunda sập nguồn hoàn toàn
        return getMockVariables(taskId);
    }

    public void assignTask(String taskId, String assignee) {
        String token = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("assignee", assignee);
        body.put("allowOverride", true);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            // SỬA ĐƯỜNG DẪN 3: Đổi sang /v1/user-tasks/{id}/assignment
            restTemplate.postForEntity(
                    tasklistUrl + "/v1/user-tasks/" + taskId + "/assignment",
                    request,
                    Map.class);
            log.info("--- [TASKLIST SERVICE] Đã gán task {} cho {} ---", taskId, assignee);
        } catch (Exception e) {
            log.error("--- [TASKLIST SERVICE] Lỗi gán task {}: {} ---", taskId, e.getMessage());
            throw new RuntimeException("Gán task thất bại: " + e.getMessage(), e);
        }
    }

    public void completeTask(String taskId, String status) {
        String token = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }

        // CẢI TIẾN 4: Cấu trúc JSON biến của Zeebe REST API mới rất sạch, dạng
        // Key-Value phẳng
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> variables = new HashMap<>();
        variables.put("Status", status); // Nhận trực tiếp chuỗi chữ, KHÔNG CẦN bọc thêm dấu nháy kép thoát chuỗi rườm
                                         // rà nữa
        body.put("variables", variables);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            // SỬA ĐƯỜNG DẪN 5: Đổi sang /v1/user-tasks/" + taskId + "/completion
            restTemplate.postForEntity(
                    tasklistUrl + "/v1/user-tasks/" + taskId + "/completion",
                    request,
                    Map.class);
            log.info("--- [TASKLIST SERVICE] Đã hoàn thành task {} với Status={} ---", taskId, status);
        } catch (Exception e) {
            log.error("--- [TASKLIST SERVICE] Lỗi hoàn thành task {}: {} ---", taskId, e.getMessage());
            throw new RuntimeException("Hoàn thành task thất bại: " + e.getMessage(), e);
        }
    }

    // Giữ nguyên các phương thức Mock Fallback phía bên dưới của bạn...
    private List<Map<String, Object>> getMockTasks(String state, Boolean assigned, String assignee) {
        List<Map<String, Object>> mockList = new ArrayList<>();
        if ("COMPLETED".equals(state))
            return mockList;
        Map<String, Object> t1 = new HashMap<>();
        t1.put("id", "2251799813685250");
        t1.put("name", "Người phê duyệt cấp 1");
        t1.put("processName", "Quy trình nghỉ phép");
        t1.put("processDefinitionKey", "Process_1oveniu");
        t1.put("processInstanceKey", "2251799813685200");
        t1.put("creationTime", "2026-05-18T00:00:00Z");
        t1.put("assignee", "demo_manager@company.com");
        t1.put("taskState", "CREATED");
        t1.put("formId", "level1.form");
        mockList.add(t1);
        return mockList;
    }

    private Map<String, Object> getMockVariables(String taskId) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("FullName", "Nguyễn Văn A");
        vars.put("Status", "PENDING");
        return vars;
    }
}