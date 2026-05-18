package com.example.demo.integration;

import com.example.demo.integration.config.ZeebeTestConfig;
import com.example.demo.integration.fixture.LeaveRequestFixture;
import com.example.demo.integration.helper.DbTestHelper;
import com.example.demo.integration.helper.ZeebeTestHelper;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * Kịch bản 2: Happy Path - Duyệt Cả Cấp 1 và Cấp 2 Thành Công (DMN Trả `need = true`)
 * ============================================================================
 *
 * Luồng chạy:
 * 1. Khởi tạo process với need0 = true
 * 2. validate-request -> isValid = true -> DB record + approval_history
 * 3. Update-request-pending -> DB status = PENDING_APPROVAL
 * 4. DMN: need0=true -> need=true
 * 5. Subprocess: 
 *    - Luồng 1: User Task level1 chờ
 *    - Luồng 2: Gateway_1br2c14 need=true -> User Task Activity_1wioexg chờ
 * 6. Complete level1 với Status = "APPROVED"
 * 7. requestupdate1 (Update-request-pending) -> Event_02gs4cq (subprocess end cho luồng 1)
 * 8. Complete Activity_1wioexg với Status = "APPROVED"
 * 9. requestupdate2 -> Event_0w8cdqo (subprocess end cho luồng 2)
 * 10. Subprocess kết thúc -> Gateway_0283bq3 Status=APPROVED -> deduct-balance-task
 * 11. Process kết thúc tại Event_1m78rjj
 *
 * DB Assertions:
 * - Sau validate: approval_history "validate-request"
 * - Sau Update-request-pending: status = PENDING_APPROVAL
 * - 2 lần duyệt: approval_history count("approval") = 2
 * - Sau deduct: used_days tăng đúng totaldays = 3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ZeebeTestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Kịch bản 2: Happy Path - Duyệt Cả 2 Cấp (need=true)")
class Scenario2HappyPathLevel1And2ApprovalTest {

    private static final Logger log = LoggerFactory.getLogger(Scenario2HappyPathLevel1And2ApprovalTest.class);

    @Autowired
    private ZeebeTestHelper zeebeHelper;

    @Autowired
    private DbTestHelper dbHelper;

    private Map<String, Object> processVariables;
    private String businessKey;
    private long processInstanceKey;

    @BeforeEach
    void setUp() {
        // need0 = true: DMN trả về need=true -> cần cả 2 cấp phê duyệt
        processVariables = LeaveRequestFixture.buildBaseVariables(true);
        businessKey = LeaveRequestFixture.extractBusinessKey(processVariables);
        log.info("[TEST][Scenario2] SetUp: businessKey={}", businessKey);
    }

    @AfterEach
    void tearDown() {
        log.info("[TEST][Scenario2] TearDown: Cleaning up businessKey={}", businessKey);
        dbHelper.cleanUpLeaveRequest(businessKey);
        dbHelper.resetUsedDays(LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
    }

    @Test
    @Order(1)
    @DisplayName("TC-02: validate -> pending -> DMN(true) -> level1 approve -> level2 approve -> deduct balance")
    void happyPath_BothLevel1And2_WithNeedTrue() {
        // =====================================================================
        // BƯỚC 1: Khởi tạo process instance
        // =====================================================================
        log.info("[TEST][Scenario2] Bước 1: Khởi tạo process instance với need0=true");
        ProcessInstanceEvent instance = zeebeHelper.startLeaveProcess(processVariables);
        processInstanceKey = instance.getProcessInstanceKey();
        assertThat(processInstanceKey).isPositive();

        // =====================================================================
        // BƯỚC 2: validate-request Worker -> DB record được tạo
        // =====================================================================
        log.info("[TEST][Scenario2] Bước 2: Đợi leave_request được tạo trong DB");
        zeebeHelper.awaitLeaveRequestExists(businessKey);

        assertThat(dbHelper.leaveRequestExists(businessKey))
                .as("leave_request phải tồn tại sau validate-request")
                .isTrue();

        // Assert approval_history action = "validate-request"
        zeebeHelper.awaitApprovalHistoryAction(businessKey, "validate-request");
        assertThat(dbHelper.approvalHistoryActionExists(businessKey, "validate-request"))
                .as("approval_histories phải có action='validate-request'")
                .isTrue();
        log.info("[TEST][Scenario2] ✅ validate-request: DB assertions passed");

        // =====================================================================
        // BƯỚC 3: Update-request-pending Worker -> status = PENDING_APPROVAL
        // =====================================================================
        log.info("[TEST][Scenario2] Bước 3: Đợi DB status=PENDING_APPROVAL");
        zeebeHelper.awaitLeaveRequestStatus(businessKey, "PENDING_APPROVAL");

        assertThat(dbHelper.getLeaveRequestStatus(businessKey))
                .as("Status phải là PENDING_APPROVAL")
                .isPresent()
                .hasValue("PENDING_APPROVAL");
        log.info("[TEST][Scenario2] ✅ Update-request-pending: DB assertions passed");

        // =====================================================================
        // BƯỚC 4: DMN need0=true -> need=true
        // Subprocess bắt đầu:
        // - Luồng 1: level1 User Task chờ
        // - Luồng 2: Activity_1wioexg User Task chờ (vì need=true)
        //
        // Complete User Task level1 với APPROVED trước
        // =====================================================================
        log.info("[TEST][Scenario2] Bước 4: Complete User Task 'level1' (cấp 1) với Status=APPROVED");
        zeebeHelper.completeUserTask("level1",
                LeaveRequestFixture.approvedPayload(),
                processInstanceKey);
        log.info("[TEST][Scenario2] ✅ User Task level1 hoàn thành");

        // =====================================================================
        // BƯỚC 5: Complete User Task Activity_1wioexg (người duyệt 2) với APPROVED
        // =====================================================================
        log.info("[TEST][Scenario2] Bước 5: Complete User Task 'Activity_1wioexg' (cấp 2) với Status=APPROVED");
        zeebeHelper.completeUserTask("Activity_1wioexg",
                LeaveRequestFixture.approvedPayload(),
                processInstanceKey);
        log.info("[TEST][Scenario2] ✅ User Task Activity_1wioexg hoàn thành");

        // Assert: 2 lần approval được ghi trong approval_histories
        // (task-complete worker ghi mỗi lần complete user task)
        // Có thể cần thời gian để DB được cập nhật
        Awaitility_awaitApprovalCount(businessKey, 2);
        int approvalCount = dbHelper.countApprovalActions(businessKey);
        assertThat(approvalCount)
                .as("Phải có đúng 2 action 'approval' trong approval_histories (cấp 1 + cấp 2)")
                .isEqualTo(2);
        log.info("[TEST][Scenario2] ✅ 2 lần duyệt được ghi nhận trong approval_histories");

        // =====================================================================
        // BƯỚC 6: Subprocess kết thúc -> deduct-balance-task chạy
        // used_days tăng thêm totaldays = 3
        // =====================================================================
        log.info("[TEST][Scenario2] Bước 6: Đợi used_days tăng sau deduct-balance-task");

        Double usedDaysBefore = dbHelper.getUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
        assertThat(usedDaysBefore).isNotNull();

        double expectedUsedDays = usedDaysBefore + LeaveRequestFixture.TOTAL_DAYS;
        zeebeHelper.awaitLeaveBalanceUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR,
                expectedUsedDays);

        Double usedDaysAfter = dbHelper.getUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
        assertThat(usedDaysAfter)
                .as("used_days phải tăng đúng %d ngày sau deduct", LeaveRequestFixture.TOTAL_DAYS)
                .isEqualTo(expectedUsedDays, org.assertj.core.data.Offset.offset(0.01));
        log.info("[TEST][Scenario2] ✅ deduct-balance-task: used_days {} -> {}", usedDaysBefore, usedDaysAfter);

        // =====================================================================
        // BƯỚC 7: Kiểm tra trạng thái cuối cùng - Event_1m78rjj
        // =====================================================================
        log.info("[TEST][Scenario2] Bước 7: Kiểm tra trạng thái cuối");
        zeebeHelper.awaitLeaveRequestFinalStatus(businessKey, "APPROVED");

        assertThat(dbHelper.getLeaveRequestStatus(businessKey))
                .as("Trạng thái cuối phải là APPROVED")
                .isPresent()
                .hasValue("APPROVED");

        log.info("[TEST][Scenario2] ✅ Kịch bản 2 HOÀN THÀNH. Process kết thúc tại Event_1m78rjj");
    }

    /**
     * Awaitility helper nội bộ: đợi approval_histories count đủ số lượng.
     */
    private void Awaitility_awaitApprovalCount(String businessKey, int expectedCount) {
        org.awaitility.Awaitility.await()
                .atMost(10, java.util.concurrent.TimeUnit.SECONDS)
                .pollInterval(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .until(() -> dbHelper.countApprovalActions(businessKey) >= expectedCount);
    }
}
