package com.example.demo.integration.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * DbTestHelper: Lớp tiện ích truy vấn DB dùng riêng trong các assertion test.
 * Tất cả query đều là read-only, dùng JdbcTemplate để truy vấn trực tiếp
 * vào PostgreSQL (không thông qua JPA Hibernate cache).
 */
@Component
public class DbTestHelper {

    private static final Logger log = LoggerFactory.getLogger(DbTestHelper.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // =========================================================================
    // LEAVE REQUEST ASSERTIONS
    // =========================================================================

    /**
     * Lấy trạng thái hiện tại của leave_request theo business_key.
     */
    public Optional<String> getLeaveRequestStatus(String businessKey) {
        List<String> result = jdbcTemplate.queryForList(
                "SELECT status FROM leave_requests WHERE business_key = ?",
                String.class, businessKey);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Lấy toàn bộ record leave_request theo business_key (dùng cho assert nhiều field).
     */
    public Optional<Map<String, Object>> getLeaveRequest(String businessKey) {
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT * FROM leave_requests WHERE business_key = ?", businessKey);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Kiểm tra leave_request tồn tại trong DB.
     */
    public boolean leaveRequestExists(String businessKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leave_requests WHERE business_key = ?",
                Integer.class, businessKey);
        return count != null && count > 0;
    }

    /**
     * Xác nhận trạng thái leave_request KHÔNG phải giá trị không mong muốn.
     */
    public boolean leaveRequestStatusIsNot(String businessKey, String unexpectedStatus) {
        Optional<String> status = getLeaveRequestStatus(businessKey);
        return status.isEmpty() || !unexpectedStatus.equals(status.get());
    }

    // =========================================================================
    // LEAVE BALANCE ASSERTIONS
    // =========================================================================

    /**
     * Lấy used_days trong leave_balances theo email nhân viên, loại phép, năm.
     *
     * @param employeeEmail Email nhân viên (key duy nhất)
     * @param leaveTypeCode Code loại phép (e.g. "ANNUAL")
     * @param year          Năm cần kiểm tra
     * @return used_days (Double) hoặc null nếu không tìm thấy
     */
    public Double getUsedDays(String employeeEmail, String leaveTypeCode, int year) {
        List<Double> result = jdbcTemplate.queryForList(
                "SELECT lb.used_days::float " +
                        "FROM leave_balances lb " +
                        "JOIN employees e ON e.id = lb.employee_id " +
                        "JOIN leave_types lt ON lt.id = lb.leave_type_id " +
                        "WHERE e.email = ? AND lt.code = ? AND lb.year = ?",
                Double.class, employeeEmail, leaveTypeCode, year);
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Lấy total_days trong leave_balances.
     */
    public Double getTotalDays(String employeeEmail, String leaveTypeCode, int year) {
        List<Double> result = jdbcTemplate.queryForList(
                "SELECT lb.total_days::float " +
                        "FROM leave_balances lb " +
                        "JOIN employees e ON e.id = lb.employee_id " +
                        "JOIN leave_types lt ON lt.id = lb.leave_type_id " +
                        "WHERE e.email = ? AND lt.code = ? AND lb.year = ?",
                Double.class, employeeEmail, leaveTypeCode, year);
        return result.isEmpty() ? null : result.get(0);
    }

    // =========================================================================
    // APPROVAL HISTORY ASSERTIONS
    // =========================================================================

    /**
     * Kiểm tra approval_histories có bản ghi action cho leave_request tương ứng.
     *
     * @param businessKey Business key của đơn phép
     * @param action      Action cần tìm (e.g. "validate-request", "approval", "reject")
     */
    public boolean approvalHistoryActionExists(String businessKey, String action) {
        List<Integer> result = jdbcTemplate.queryForList(
                "SELECT 1 FROM approval_histories ah " +
                        "JOIN leave_requests lr ON lr.id = ah.leave_request_id " +
                        "WHERE lr.business_key = ? AND ah.action = ?",
                Integer.class, businessKey, action);
        return !result.isEmpty();
    }

    /**
     * Lấy danh sách tất cả action trong approval_histories của đơn phép.
     */
    public List<String> getApprovalHistoryActions(String businessKey) {
        return jdbcTemplate.queryForList(
                "SELECT ah.action FROM approval_histories ah " +
                        "JOIN leave_requests lr ON lr.id = ah.leave_request_id " +
                        "WHERE lr.business_key = ? ORDER BY ah.created_at",
                String.class, businessKey);
    }

    /**
     * Lấy số lần approve của một đơn phép (đếm action = "approval").
     */
    public int countApprovalActions(String businessKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_histories ah " +
                        "JOIN leave_requests lr ON lr.id = ah.leave_request_id " +
                        "WHERE lr.business_key = ? AND ah.action = 'approval'",
                Integer.class, businessKey);
        return count != null ? count : 0;
    }

    // =========================================================================
    // EMPLOYEE HELPERS
    // =========================================================================

    /**
     * Lấy UUID của employee theo email (dùng cho nhiều query liên quan).
     */
    public Optional<UUID> getEmployeeId(String email) {
        List<String> result = jdbcTemplate.queryForList(
                "SELECT id::text FROM employees WHERE email = ?",
                String.class, email);
        return result.isEmpty() ? Optional.empty()
                : Optional.of(UUID.fromString(result.get(0)));
    }

    // =========================================================================
    // TEST DATA SETUP / TEARDOWN (dùng @BeforeEach / @AfterEach)
    // =========================================================================

    /**
     * Xóa leave_request theo business_key và cascade các bản ghi liên quan.
     * Dùng trong @AfterEach để dọn dẹp test data.
     */
    public void cleanUpLeaveRequest(String businessKey) {
        log.info("[TEST-CLEANUP] Deleting leave_request & related data for businessKey='{}'", businessKey);
        // Xóa approval_histories trước (không có FK cascade)
        jdbcTemplate.update(
                "DELETE FROM approval_histories " +
                        "WHERE leave_request_id IN " +
                        "(SELECT id FROM leave_requests WHERE business_key = ?)",
                businessKey);
        // Xóa leave_request
        jdbcTemplate.update("DELETE FROM leave_requests WHERE business_key = ?", businessKey);
        log.info("[TEST-CLEANUP] Done for businessKey='{}'", businessKey);
    }

    /**
     * Reset used_days về 0 cho nhân viên sau khi chạy test deduct balance.
     */
    public void resetUsedDays(String employeeEmail, String leaveTypeCode, int year) {
        log.info("[TEST-CLEANUP] Resetting used_days for employee='{}', leaveType='{}', year={}",
                employeeEmail, leaveTypeCode, year);
        jdbcTemplate.update(
                "UPDATE leave_balances lb " +
                        "SET used_days = 0, pending_days = 0 " +
                        "FROM employees e, leave_types lt " +
                        "WHERE lb.employee_id = e.id " +
                        "AND lb.leave_type_id = lt.id " +
                        "AND e.email = ? AND lt.code = ? AND lb.year = ?",
                employeeEmail, leaveTypeCode, year);
        log.info("[TEST-CLEANUP] used_days reset done.");
    }
}
