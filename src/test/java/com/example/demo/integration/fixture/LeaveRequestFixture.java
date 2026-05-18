package com.example.demo.integration.fixture;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * LeaveRequestFixture: Cung cấp dữ liệu nghiệp vụ chuẩn dùng trong tất cả test
 * cases.
 *
 * Cấu trúc form data khớp 100% với usersubmitform.form và process variables.
 */
public final class LeaveRequestFixture {

    private LeaveRequestFixture() {
    }

    // =========================================================================
    // NHÂN VIÊN THỬ NGHIỆM (phải tồn tại trong DB - được seed bởi DatabaseSeeder)
    // =========================================================================
    /** Email nhân viên thử nghiệm (phải khớp với dữ liệu seed). */
    public static final String EMPLOYEE_EMAIL = "nguyennhatquang52004@gmail.com";
    public static final String EMPLOYEE_ID_CODE = "DM-2026";
    public static final String EMPLOYEE_FULL_NAME = "Nguyễn Nhật Quang";
    public static final String LEAVE_TYPE_CODE = "ANNUAL";
    public static final int LEAVE_YEAR = 2026;

    // =========================================================================
    // TẠO BIẾN QUY TRÌNH (BPMN PROCESS VARIABLES)
    // =========================================================================

    /**
     * Tạo map biến đầy đủ cho 1 đơn xin phép chuẩn.
     * businessKey được sinh ngẫu nhiên để mỗi test case độc lập.
     *
     * @param need0 Giá trị input DMN: true = cần duyệt cấp 2, false = không cần
     * @return Map<String, Object> variables
     */
    public static Map<String, Object> buildBaseVariables(boolean need0) {
        String uniqueBusinessKey = "BK-TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return buildVariables(need0, uniqueBusinessKey);
    }

    /**
     * Tạo map biến với businessKey chỉ định (dùng khi cần correlation key cố định).
     */
    public static Map<String, Object> buildVariables(boolean need0, String businessKey) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("EmployeeID", EMPLOYEE_ID_CODE);
        vars.put("FullName", EMPLOYEE_FULL_NAME);
        vars.put("Department", "IT");
        vars.put("LeaveType", LEAVE_TYPE_CODE);
        vars.put("StartDate", "2026-05-16T08:00:00Z");
        vars.put("EndDate", "2026-05-19T17:00:00Z");
        vars.put("leaveSession", "ALLDAY");
        vars.put("totaldays", 3);
        vars.put("reason", "Nghỉ phép hệ thống - Integration Test [" + businessKey + "]");
        vars.put("Status", "PENDING");
        // need0: input DMN rule để quyết định có cần duyệt cấp 2 không
        vars.put("need0", need0);
        // businessKey: dùng làm correlation key cho message cancel
        vars.put("businessKey", businessKey);
        return vars;
    }

    /**
     * Tạo payload hoàn thành User Task với Status = APPROVED.
     */
    public static Map<String, Object> approvedPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("Status", "APPROVED");
        return payload;
    }

    /**
     * Tạo payload hoàn thành User Task với Status = Rejected.
     * CHÚ Ý: Giá trị chính xác theo BPMN condition: =Status ="Rejected" (viết hoa
     * R).
     */
    public static Map<String, Object> rejectedPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("Status", "Rejected");
        return payload;
    }

    /**
     * Tạo payload message cancel kèm lý do.
     */
    public static Map<String, Object> cancelMessagePayload(String cancelReason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cancelReason", cancelReason);
        return payload;
    }

    /**
     * Lấy businessKey từ variable map.
     */
    public static String extractBusinessKey(Map<String, Object> variables) {
        return (String) variables.get("businessKey");
    }

    /**
     * Số ngày nghỉ chuẩn trong fixture này (dùng để tính toán DB assertion).
     */
    public static final int TOTAL_DAYS = 3;
}
