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
 * Kịch bản 1: Happy Path - Duyệt Cấp 1 Thành Công (DMN Trả `need = false`)
 * ============================================================================
 *
 * Luồng chạy:
 * 1. Khởi tạo process với need0 = false
 * 2. Worker validate-request chạy -> isValid = true -> DB có record + approval_history
 * 3. Worker Update-request-pending chạy -> DB status = PENDING_APPROVAL
 * 4. DMN rule base xử lý need0=false -> need=false
 * 5. Gateway_0fhpbp0: isvaildrule=true -> vào Subprocess Activity_0qizs4k
 * 6. Trong subprocess: luồng parallel chạy
 *    - Luồng 1: User Task `level1` chờ
 *    - Luồng 2: Gateway_1br2c14 với need=false -> bẻ thẳng Event_0h23kml (end trong subprocess)
 * 7. Complete User Task level1 với Status = "APPROVED"
 * 8. Subprocess kết thúc -> Gateway_0283bq3 với Status="APPROVED" -> Worker deduct-balance-task
 * 9. Process kết thúc tại Event_1m78rjj
 *
 * DB Assertions:
 * - Sau validate: approval_history có record action="validate-request"
 * - Sau Update-request-pending: status = PENDING_APPROVAL
 * - Sau deduct-balance-task: used_days tăng lên đúng totaldays=3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ZeebeTestConfig.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Kịch bản 1: Happy Path - Duyệt Cấp 1 (need=false)")
class Scenario1HappyPathLevel1ApprovalTest {

    private static final Logger log = LoggerFactory.getLogger(Scenario1HappyPathLevel1ApprovalTest.class);

    @Autowired
    private ZeebeTestHelper zeebeHelper;

    @Autowired
    private DbTestHelper dbHelper;

    // Biến dùng chung trong test instance (JUnit 5 lifecycle = PER_METHOD)
    private Map<String, Object> processVariables;
    private String businessKey;
    private long processInstanceKey;

    @BeforeEach
    void setUp() {
        // Tạo biến process với need0 = false (DMN sẽ trả need = false)
        processVariables = LeaveRequestFixture.buildBaseVariables(false);
        businessKey = LeaveRequestFixture.extractBusinessKey(processVariables);
        log.info("[TEST][Scenario1] SetUp: businessKey={}", businessKey);
    }

    @AfterEach
    void tearDown() {
        log.info("[TEST][Scenario1] TearDown: Cleaning up businessKey={}", businessKey);
        dbHelper.cleanUpLeaveRequest(businessKey);
        dbHelper.resetUsedDays(LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
    }

    @Test
    @Order(1)
    @DisplayName("TC-01: Luồng hoàn chỉnh - validate -> pending -> DMN(false) -> level1 approve -> deduct balance")
    void happyPath_Level1Approval_WithNeedFalse() {
        // =====================================================================
        // BƯỚC 1: Khởi tạo process instance
        // =====================================================================
        log.info("[TEST][Scenario1] Bước 1: Khởi tạo process instance");
        ProcessInstanceEvent instance = zeebeHelper.startLeaveProcess(processVariables);
        processInstanceKey = instance.getProcessInstanceKey();
        assertThat(processInstanceKey).isPositive();
        log.info("[TEST][Scenario1] Process instance started: key={}", processInstanceKey);

        // =====================================================================
        // BƯỚC 2: Sau validate-request Worker
        // Worker leaveValidateRequestUseCase.validate() tạo record trong DB
        // và ghi approval_history action="validate-request"
        // =====================================================================
        log.info("[TEST][Scenario1] Bước 2: Đợi DB có record leave_request sau validate-request");
        zeebeHelper.awaitLeaveRequestExists(businessKey);

        // Assert: Record đã được tạo trong leave_requests
        assertThat(dbHelper.leaveRequestExists(businessKey))
                .as("leave_request phải tồn tại trong DB sau validate-request")
                .isTrue();

        // Assert: approval_histories có action="validate-request"
        zeebeHelper.awaitApprovalHistoryAction(businessKey, "validate-request");
        assertThat(dbHelper.approvalHistoryActionExists(businessKey, "validate-request"))
                .as("approval_histories phải có action='validate-request'")
                .isTrue();
        log.info("[TEST][Scenario1] ✅ validate-request: DB assertions passed");

        // =====================================================================
        // BƯỚC 3: Sau Update-request-pending Worker
        // Status chuyển sang PENDING_APPROVAL
        // =====================================================================
        log.info("[TEST][Scenario1] Bước 3: Đợi DB status=PENDING_APPROVAL sau Update-request-pending");
        zeebeHelper.awaitLeaveRequestStatus(businessKey, "PENDING_APPROVAL");

        Optional<String> statusAfterPending = dbHelper.getLeaveRequestStatus(businessKey);
        assertThat(statusAfterPending)
                .as("DB status phải là PENDING_APPROVAL")
                .isPresent()
                .hasValue("PENDING_APPROVAL");
        log.info("[TEST][Scenario1] ✅ Update-request-pending: DB assertions passed");

        // =====================================================================
        // BƯỚC 4 & 5: DMN xử lý need0=false -> need=false
        // Subprocess: luồng 1 chờ tại User Task level1,
        //             luồng 2 qua Gateway_1br2c14 -> Event_0h23kml (end trong subprocess)
        //
        // Ta hoàn thành User Task level1 với Status = "APPROVED"
        // =====================================================================
        log.info("[TEST][Scenario1] Bước 4: Complete User Task 'level1' với Status=APPROVED");
        zeebeHelper.completeUserTask("level1",
                LeaveRequestFixture.approvedPayload(),
                processInstanceKey);
        log.info("[TEST][Scenario1] ✅ User Task level1 hoàn thành");

        // =====================================================================
        // BƯỚC 5: Sau subprocess kết thúc, Gateway_0283bq3 với Status=APPROVED
        // -> deduct-balance-task Worker chạy
        // -> used_days tăng thêm totaldays = 3
        // =====================================================================
        log.info("[TEST][Scenario1] Bước 5: Đợi used_days tăng sau deduct-balance-task");

        // Lấy used_days hiện tại trước khi deduct (để tính expected)
        Double usedDaysBefore = dbHelper.getUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
        assertThat(usedDaysBefore).as("used_days trước deduct phải có giá trị").isNotNull();

        double expectedUsedDaysAfterDeduct = usedDaysBefore + LeaveRequestFixture.TOTAL_DAYS;
        zeebeHelper.awaitLeaveBalanceUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR,
                expectedUsedDaysAfterDeduct);

        Double usedDaysAfter = dbHelper.getUsedDays(
                LeaveRequestFixture.EMPLOYEE_EMAIL,
                LeaveRequestFixture.LEAVE_TYPE_CODE,
                LeaveRequestFixture.LEAVE_YEAR);
        assertThat(usedDaysAfter)
                .as("used_days sau deduct phải tăng thêm đúng %d ngày", LeaveRequestFixture.TOTAL_DAYS)
                .isEqualTo(expectedUsedDaysAfterDeduct, org.assertj.core.data.Offset.offset(0.01));
        log.info("[TEST][Scenario1] ✅ deduct-balance-task: used_days từ {} -> {}", usedDaysBefore, usedDaysAfter);

        // =====================================================================
        // BƯỚC 6: Assert trạng thái cuối cùng trong DB
        // Process kết thúc tại Event_1m78rjj -> status cuối là APPROVED
        // =====================================================================
        log.info("[TEST][Scenario1] Bước 6: Kiểm tra trạng thái cuối cùng");
        zeebeHelper.awaitLeaveRequestFinalStatus(businessKey, "APPROVED");

        Optional<String> finalStatus = dbHelper.getLeaveRequestStatus(businessKey);
        assertThat(finalStatus)
                .as("Trạng thái cuối cùng phải là APPROVED (process kết thúc tại Event_1m78rjj)")
                .isPresent()
                .hasValue("APPROVED");

        log.info("[TEST][Scenario1] ✅ Kịch bản 1 HOÀN THÀNH. Process kết thúc tại Event_1m78rjj");
    }
}
